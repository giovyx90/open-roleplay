package dev.openrp.crime.module.racket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.EscalationLevel;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.core.Ids;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Protection;
import dev.openrp.crime.model.ProtectionStatus;

/**
 * Drives extortion over companies. Imposition is physical (a member faces the owner); payment runs on
 * a timer when the owner has funds, otherwise an agent must collect physically. Escalation level 3
 * carries no automatic effect - it is a narrative "from here on it is unassisted RP" signal. The
 * plugin never destroys a company. Requires a {@code CompanyAdapter}; without one it stays inert.
 */
public final class RacketService {

    private final OpenCrimePlugin plugin;
    private final java.util.Map<String, Protection> byId = new ConcurrentHashMap<>();

    public RacketService(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byId.clear();
        for (Protection protection : plugin.adapters().storage().loadProtections()) {
            byId.put(protection.id(), protection);
        }
    }

    public boolean companiesReady() {
        return plugin.adapters().company().available();
    }

    public CrimeResult impose(Player member, Player owner) {
        if (!companiesReady()) {
            return CrimeResult.fail("racket.no_companies");
        }
        IllegalOrg org = plugin.orgs().byMember(member.getUniqueId()).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(member.getUniqueId(), Capability.RACKET_IMPOSE)) {
            return CrimeResult.fail("general.no_capability");
        }
        Optional<String> company = plugin.adapters().company().companyOwnedBy(owner.getUniqueId());
        if (company.isEmpty()) {
            return CrimeResult.fail("racket.no_company", "player", owner.getName());
        }
        if (activeFor(company.get()) != null) {
            return CrimeResult.fail("racket.already_protected");
        }
        Protection protection = new Protection(Ids.prefixed("prot"), org.id(), company.get(),
                plugin.config().racket().defaultAmount(), plugin.config().racket().defaultPeriodDays());
        byId.put(protection.id(), protection);
        plugin.adapters().storage().saveProtection(protection);

        plugin.adapters().notification().send(owner, plugin.messages().prefixed(owner,
                "racket.owner_notified", "org", org.name(), "amount", String.valueOf(protection.amount()),
                "id", protection.id()));
        return CrimeResult.ok("racket.imposed", "company",
                plugin.adapters().company().companyName(company.get()), "id", protection.id());
    }

    public CrimeResult respond(Player owner, String protectionId, boolean accept) {
        Protection protection = byId.get(protectionId);
        if (protection == null) {
            return CrimeResult.fail("racket.unknown_contract", "id", String.valueOf(protectionId));
        }
        if (!plugin.adapters().company().isOwner(owner.getUniqueId(), protection.companyId())) {
            return CrimeResult.fail("racket.not_owner");
        }
        if (protection.status() != ProtectionStatus.PENDING) {
            return CrimeResult.fail("racket.not_pending");
        }
        if (accept) {
            protection.setStatus(ProtectionStatus.ACTIVE);
            protection.setNextDue(System.currentTimeMillis()
                    + plugin.config().settings().realMillisFromDays(protection.periodDays()));
        } else {
            protection.setStatus(ProtectionStatus.REFUSED);
        }
        plugin.adapters().storage().saveProtection(protection);
        return CrimeResult.ok(accept ? "racket.accepted" : "racket.refused");
    }

    public CrimeResult collect(Player member, String protectionId) {
        IllegalOrg org = plugin.orgs().byMember(member.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(member.getUniqueId(), Capability.RACKET_COLLECT)) {
            return CrimeResult.fail("general.no_capability");
        }
        Protection protection = byId.get(protectionId);
        if (protection == null || !protection.orgId().equals(org.id())) {
            return CrimeResult.fail("racket.unknown_contract", "id", String.valueOf(protectionId));
        }
        // Manual collection is the fallback for a contract whose auto-payment failed (OVERDUE) or whose
        // period has elapsed (ACTIVE and due). It is never an unlimited charge button, and never applies
        // to a PENDING / REFUSED / REVOKED contract.
        long now = System.currentTimeMillis();
        boolean collectable = protection.status() == ProtectionStatus.OVERDUE
                || (protection.status() == ProtectionStatus.ACTIVE && protection.isDue(now));
        if (!collectable) {
            return CrimeResult.fail("racket.not_due");
        }
        if (!plugin.adapters().company().chargeCompany(protection.companyId(), protection.amount())) {
            return CrimeResult.fail("racket.cannot_pay");
        }
        creditAndSchedule(org, protection, member.getUniqueId(), member);
        return CrimeResult.ok("racket.collected", "amount", String.valueOf(protection.amount()));
    }

    public CrimeResult escalate(Player member, String protectionId) {
        IllegalOrg org = plugin.orgs().byMember(member.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(member.getUniqueId(), Capability.RACKET_ESCALATE)) {
            return CrimeResult.fail("general.no_capability");
        }
        Protection protection = byId.get(protectionId);
        if (protection == null || !protection.orgId().equals(org.id())) {
            return CrimeResult.fail("racket.unknown_contract", "id", String.valueOf(protectionId));
        }
        int next = protection.coercionLevel() + 1;
        EscalationLevel level = plugin.config().racket().level(next).orElse(null);
        if (level == null) {
            return CrimeResult.fail("racket.max_escalation");
        }
        protection.setCoercionLevel(next);
        plugin.adapters().storage().saveProtection(protection);
        applyEscalation(protection, level);
        return CrimeResult.ok("racket.escalated", "level", String.valueOf(next), "name", level.name());
    }

    public CrimeResult revoke(Player member, String protectionId) {
        IllegalOrg org = plugin.orgs().byMember(member.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(member.getUniqueId(), Capability.RACKET_MANAGE)) {
            return CrimeResult.fail("general.no_capability");
        }
        Protection protection = byId.get(protectionId);
        if (protection == null || !protection.orgId().equals(org.id())) {
            return CrimeResult.fail("racket.unknown_contract", "id", String.valueOf(protectionId));
        }
        protection.setStatus(ProtectionStatus.REVOKED);
        plugin.adapters().storage().saveProtection(protection);
        return CrimeResult.ok("racket.revoked");
    }

    public List<Protection> ofOrg(String orgId) {
        List<Protection> result = new ArrayList<>();
        for (Protection protection : byId.values()) {
            if (protection.orgId().equals(orgId) && protection.status() != ProtectionStatus.REVOKED) {
                result.add(protection);
            }
        }
        return result;
    }

    /** Periodic billing: charges due contracts when the owner can pay, else flags them overdue. */
    public void billDue() {
        if (!plugin.config().settings().racketPaymentAuto()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Protection protection : byId.values()) {
            if (!protection.isDue(now)) {
                continue;
            }
            IllegalOrg org = plugin.orgs().get(protection.orgId()).orElse(null);
            if (org == null) {
                continue;
            }
            if (plugin.adapters().company().chargeCompany(protection.companyId(), protection.amount())) {
                creditAndSchedule(org, protection, null, null);
            } else {
                protection.setStatus(ProtectionStatus.OVERDUE);
                plugin.adapters().storage().saveProtection(protection);
            }
        }
    }

    private void creditAndSchedule(IllegalOrg org, Protection protection, java.util.UUID collector, Player at) {
        plugin.adapters().economy().deposit(org.treasury(), protection.amount(), true);
        protection.setStatus(ProtectionStatus.ACTIVE);
        protection.setLastPayment(System.currentTimeMillis());
        protection.setNextDue(System.currentTimeMillis()
                + plugin.config().settings().realMillisFromDays(protection.periodDays()));
        plugin.adapters().storage().saveProtection(protection);

        String world = at == null ? "" : at.getWorld().getName();
        int x = at == null ? 0 : at.getLocation().getBlockX();
        int y = at == null ? 0 : at.getLocation().getBlockY();
        int z = at == null ? 0 : at.getLocation().getBlockZ();
        CrimeEvent event = new CrimeEvent(Ids.prefixed("evt"), CrimeEventType.EXTORTION, org.id(),
                collector == null ? List.of() : List.of(collector), List.of(), world, x, y, z,
                System.currentTimeMillis(), null);
        plugin.events().register(event);
    }

    private void applyEscalation(Protection protection, EscalationLevel level) {
        switch (level.effect() == null ? "none" : level.effect().toLowerCase(java.util.Locale.ROOT)) {
            case "company_reputation_malus" ->
                    plugin.adapters().company().applyReputationMalus(protection.companyId(), level.reputationMalus());
            case "notification_to_owner" -> notifyOwner(protection, level);
            default -> {
                // level "none" is a pure narrative signal - the plugin does nothing automatic.
            }
        }
    }

    private void notifyOwner(Protection protection, EscalationLevel level) {
        // Best-effort: notify the owner if they happen to be online.
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.adapters().company().isOwner(online.getUniqueId(), protection.companyId())) {
                plugin.adapters().notification().send(online, plugin.messages().prefixed(online,
                        "racket.escalation_notice", "level", level.name()));
            }
        }
    }

    private Protection activeFor(String companyId) {
        for (Protection protection : byId.values()) {
            if (protection.companyId().equals(companyId)
                    && protection.status() != ProtectionStatus.REVOKED
                    && protection.status() != ProtectionStatus.REFUSED) {
                return protection;
            }
        }
        return null;
    }
}
