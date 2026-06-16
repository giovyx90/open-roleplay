package dev.openrp.companies.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.api.CompanyService;
import dev.openrp.companies.event.CompanyCreateEvent;
import dev.openrp.companies.event.CompanyDeleteEvent;
import dev.openrp.companies.event.CompanyMemberInviteEvent;
import dev.openrp.companies.event.CompanyMemberJoinEvent;
import dev.openrp.companies.event.CompanyMemberLeaveEvent;
import dev.openrp.companies.event.CompanyRoleChangeEvent;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyCapability;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.companies.model.PendingInvite;

/**
 * Bukkit-facing implementation of {@link CompanyService}. Delegates all rules and state to the pure
 * {@link CompanyManager} and adds the framework concerns on top: per-company locking, firing the
 * (cancellable) lifecycle events, charging the economy adapter for creation, refreshing player
 * identity, sending notifications and writing the audit log. The split keeps the rule engine testable
 * while this class wires it to the server.
 */
public final class DefaultCompanyService implements CompanyService {

    private final OpenCompaniesPlugin plugin;

    public DefaultCompanyService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    private CompanyManager manager() {
        return plugin.companyManager();
    }

    // --- queries -----------------------------------------------------------------------------

    @Override
    public Optional<Company> findById(String companyId) {
        return manager().company(companyId);
    }

    @Override
    public Optional<Company> findByName(String name) {
        return manager().byName(name);
    }

    @Override
    public List<Company> findByPlayer(UUID playerUuid) {
        return manager().byPlayer(playerUuid);
    }

    @Override
    public Collection<Company> allCompanies() {
        return manager().all();
    }

    @Override
    public boolean exists(String companyId) {
        return manager().exists(companyId);
    }

    @Override
    public Optional<CompanyRole> roleOf(UUID playerUuid, String companyId) {
        return manager().roleOf(playerUuid, companyId);
    }

    @Override
    public boolean hasCapability(UUID playerUuid, String companyId, CompanyCapability capability) {
        return manager().hasCapability(playerUuid, companyId, capability);
    }

    @Override
    public Optional<PendingInvite> pendingInvite(UUID playerUuid) {
        return manager().pendingInvite(playerUuid);
    }

    // --- lifecycle ---------------------------------------------------------------------------

    @Override
    public CompanyResult createCompany(UUID ownerUuid, String ownerName, String displayName, String type) {
        ReentrantLock lock = plugin.locks().get(CompanyValidator.slugify(displayName));
        lock.lock();
        try {
            CompanyResult result = manager().createAsAdmin(ownerUuid, ownerName, displayName, type);
            if (result.failed()) {
                return result;
            }
            Company company = result.company().orElseThrow();
            if (fireCancelled(new CompanyCreateEvent(null, company))) {
                manager().delete(company.id());
                return CompanyResult.fail("creation.cancelled");
            }
            audit("CREATE", "Company '" + company.id() + "' created (admin/api) owner=" + ownerName);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult createCompanyForPlayer(Player creator, String displayName, String type) {
        UUID ownerUuid = creator.getUniqueId();
        ReentrantLock lock = plugin.locks().get(CompanyValidator.slugify(displayName));
        lock.lock();
        try {
            CompanyResult check = manager().canCreateForPlayer(ownerUuid, displayName, type);
            if (check.failed()) {
                return check;
            }
            double cost = plugin.settings().creationCost();
            String account = plugin.settings().economyAccount();
            if (cost > 0 && !plugin.adapters().economy().withdraw(creator, account, cost)) {
                return CompanyResult.fail("creation.cannot_afford", "cost", format(cost));
            }
            CompanyResult result = manager().createForPlayer(ownerUuid, creator.getName(), displayName, type);
            if (result.failed()) {
                refund(creator, account, cost);
                return result;
            }
            Company company = result.company().orElseThrow();
            if (fireCancelled(new CompanyCreateEvent(creator, company))) {
                manager().delete(company.id());
                refund(creator, account, cost);
                return CompanyResult.fail("creation.cancelled");
            }
            company.owner().ifPresent(owner -> plugin.adapters().identity().applyIdentity(creator, company, owner));
            audit("CREATE", "Company '" + company.id() + "' founded by " + creator.getName());
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult deleteCompany(String companyId) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            if (fireCancelled(new CompanyDeleteEvent(company))) {
                return CompanyResult.fail("general.cancelled");
            }
            clearIdentities(company);
            plugin.assetManager().removeAllOf(company.id());
            plugin.ledger().removeAllOf(company.id());
            plugin.recurring().removeAllOf(company.id());
            CompanyResult result = manager().delete(company.id());
            // The company's lock is intentionally retained (see CompanyLocks): evicting it here while
            // held would let a concurrent same-id creation acquire a different lock and race the delete.
            audit("DELETE", "Company '" + company.id() + "' deleted");
            return result;
        } finally {
            lock.unlock();
        }
    }

    // --- membership --------------------------------------------------------------------------

    @Override
    public CompanyResult inviteMember(String companyId, UUID inviterUuid, UUID targetUuid, String targetName,
                                      CompanyRole role) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyResult result = manager().invite(companyId, inviterUuid, targetUuid, targetName, role);
            if (result.failed()) {
                return result;
            }
            if (fireCancelled(new CompanyMemberInviteEvent(company, inviterUuid, targetUuid, role))) {
                manager().denyInvite(targetUuid);
                return CompanyResult.fail("general.cancelled");
            }
            Player target = plugin.getServer().getPlayer(targetUuid);
            if (target != null) {
                plugin.adapters().notification().notify(target, plugin.messages().mini(target,
                        "invite.received", "company", company.displayName(), "role",
                        plugin.messages().text(target, role.messageKey())));
            }
            audit("INVITE", "Player " + targetUuid + " invited to '" + company.id() + "' as " + role);
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult acceptInvite(UUID playerUuid, String playerName) {
        Optional<PendingInvite> pending = manager().pendingInvite(playerUuid);
        if (pending.isEmpty()) {
            return CompanyResult.fail("invite.none");
        }
        ReentrantLock lock = plugin.locks().get(pending.get().companyId());
        lock.lock();
        try {
            CompanyResult result = manager().acceptInvite(playerUuid, playerName);
            if (result.success()) {
                result.member().ifPresent(member -> {
                    Company company = manager().company(member.companyId()).orElse(null);
                    if (company != null) {
                        fire(new CompanyMemberJoinEvent(company, member));
                        Player player = plugin.getServer().getPlayer(playerUuid);
                        if (player != null) {
                            plugin.adapters().identity().applyIdentity(player, company, member);
                        }
                    }
                    audit("JOIN", playerName + " joined '" + member.companyId() + "' as " + member.role());
                });
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult denyInvite(UUID playerUuid) {
        return manager().denyInvite(playerUuid);
    }

    @Override
    public CompanyResult removeMember(String companyId, UUID actorUuid, UUID targetUuid) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            boolean selfLeave = actorUuid != null && actorUuid.equals(targetUuid);
            CompanyResult result = manager().removeMember(companyId, actorUuid, targetUuid);
            if (result.success()) {
                result.member().ifPresent(member -> {
                    CompanyMemberLeaveEvent.Reason reason = selfLeave
                            ? CompanyMemberLeaveEvent.Reason.LEFT
                            : CompanyMemberLeaveEvent.Reason.FIRED;
                    fire(new CompanyMemberLeaveEvent(company, member, reason));
                    Player target = plugin.getServer().getPlayer(member.playerUuid());
                    if (target != null) {
                        plugin.adapters().identity().clearIdentity(target, company);
                    }
                    audit("LEAVE", member.playerName() + " " + (selfLeave ? "left" : "was fired from") + " '"
                            + company.id() + "'");
                });
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult changeRole(String companyId, UUID actorUuid, UUID targetUuid, CompanyRole newRole) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyRole previousRole = company.member(targetUuid).map(CompanyMember::role).orElse(null);
            CompanyResult result = manager().changeRole(companyId, actorUuid, targetUuid, newRole);
            if (result.success()) {
                result.member().ifPresent(member -> {
                    fire(new CompanyRoleChangeEvent(company, member, previousRole, newRole));
                    Player target = plugin.getServer().getPlayer(member.playerUuid());
                    if (target != null) {
                        plugin.adapters().identity().applyIdentity(target, company, member);
                    }
                    audit("ROLE", member.playerName() + " in '" + company.id() + "' set to " + newRole);
                });
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult transferOwnership(String companyId, UUID newOwnerUuid, String newOwnerName) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyResult result = manager().setOwner(companyId, newOwnerUuid, newOwnerName);
            if (result.success()) {
                Player newOwner = plugin.getServer().getPlayer(newOwnerUuid);
                if (newOwner != null) {
                    company.member(newOwnerUuid).ifPresent(member ->
                            plugin.adapters().identity().applyIdentity(newOwner, company, member));
                }
                audit("OWNER", "Company '" + company.id() + "' ownership -> " + newOwnerName);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    // --- helpers -----------------------------------------------------------------------------

    private void clearIdentities(Company company) {
        for (CompanyMember member : company.members()) {
            Player online = plugin.getServer().getPlayer(member.playerUuid());
            if (online != null) {
                plugin.adapters().identity().clearIdentity(online, company);
            }
        }
    }

    private void refund(Player player, String account, double cost) {
        if (cost > 0) {
            plugin.adapters().economy().deposit(player, account, cost);
        }
    }

    private void fire(Event event) {
        plugin.getServer().getPluginManager().callEvent(event);
    }

    private boolean fireCancelled(CompanyCreateEvent event) {
        fire(event);
        return event.isCancelled();
    }

    private boolean fireCancelled(CompanyDeleteEvent event) {
        fire(event);
        return event.isCancelled();
    }

    private boolean fireCancelled(CompanyMemberInviteEvent event) {
        fire(event);
        return event.isCancelled();
    }

    private void audit(String category, String message) {
        plugin.adapters().logging().log(category, message);
    }

    private String format(double value) {
        return plugin.settings().currencySymbol() + String.format("%.2f", value);
    }
}
