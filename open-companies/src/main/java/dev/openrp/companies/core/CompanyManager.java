package dev.openrp.companies.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.StorageAdapter;
import dev.openrp.companies.config.CompaniesSettings;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyCapability;
import dev.openrp.companies.model.CompanyLicenseStatus;
import dev.openrp.companies.model.CompanyLicenseType;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.companies.model.CompanyStatus;
import dev.openrp.companies.model.PendingInvite;

/**
 * In-memory registry and rule engine for companies, membership, invitations and the chamber-of-
 * commerce data (applications, licenses, status, headquarters). This is the testable heart of the
 * module: it is pure Java - no Bukkit, no events, no economy, no I/O beyond the injected
 * {@link StorageAdapter} - so every invariant (creation modes, limits, role checks, invite/accept
 * flow, role changes) can be unit-tested directly.
 *
 * <p>Per-company invariants are serialized by the services through {@code CompanyLocks}. The backing
 * maps are nonetheless {@link ConcurrentHashMap}s because read-only callers (e.g. the vending-machine
 * integration) may query them off the main thread while the main thread mutates them; concurrent maps
 * give those readers a consistent structure and weakly-consistent iteration instead of corruption or
 * {@code ConcurrentModificationException}. Mutating methods persist through the storage adapter and
 * return a {@link CompanyResult}; the Bukkit-facing services add events, notifications and audit
 * logging on top.</p>
 */
public final class CompanyManager {

    private final CompaniesSettings settings;
    private final CompanyValidator validator;
    private final AdapterRegistry adapters;

    private final Map<String, Company> companies = new ConcurrentHashMap<>();
    private final Map<UUID, CompanyApplication> applications = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInvite> invites = new ConcurrentHashMap<>();
    private final Map<UUID, Long> creationCooldowns = new ConcurrentHashMap<>();

    public CompanyManager(CompaniesSettings settings, CompanyValidator validator, AdapterRegistry adapters) {
        this.settings = settings;
        this.validator = validator;
        this.adapters = adapters;
    }

    /** The active storage backend, read live so a swapped adapter takes effect immediately. */
    private StorageAdapter storage() {
        return adapters.storage();
    }

    /** Loads companies and applications from storage into memory. Invites/cooldowns stay in memory. */
    public void loadAll() {
        companies.clear();
        applications.clear();
        for (Company company : storage().loadCompanies()) {
            companies.put(company.id(), company);
        }
        for (CompanyApplication application : storage().loadApplications()) {
            applications.put(application.id(), application);
        }
    }

    // --- queries -----------------------------------------------------------------------------

    public Optional<Company> company(String companyId) {
        return companyId == null ? Optional.empty() : Optional.ofNullable(companies.get(normalizeId(companyId)));
    }

    public boolean exists(String companyId) {
        return company(companyId).isPresent();
    }

    public Optional<Company> byName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Optional<Company> byId = company(CompanyValidator.slugify(name));
        if (byId.isPresent()) {
            return byId;
        }
        String needle = name.trim();
        return companies.values().stream()
                .filter(company -> company.displayName().equalsIgnoreCase(needle))
                .findFirst();
    }

    public List<Company> byPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }
        List<Company> result = new ArrayList<>();
        for (Company company : companies.values()) {
            if (company.isMember(playerUuid)) {
                result.add(company);
            }
        }
        return result;
    }

    public Collection<Company> all() {
        return Collections.unmodifiableCollection(companies.values());
    }

    public Optional<CompanyRole> roleOf(UUID playerUuid, String companyId) {
        return company(companyId).flatMap(company -> company.member(playerUuid)).map(CompanyMember::role);
    }

    public boolean hasCapability(UUID playerUuid, String companyId, CompanyCapability capability) {
        return company(companyId)
                .flatMap(company -> company.member(playerUuid))
                .map(member -> member.role().grants(capability))
                .orElse(false);
    }

    public Optional<PendingInvite> pendingInvite(UUID playerUuid) {
        return playerUuid == null ? Optional.empty() : Optional.ofNullable(invites.get(playerUuid));
    }

    public long ownedCount(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        return companies.values().stream()
                .filter(company -> playerUuid.equals(company.ownerUuid()))
                .filter(company -> company.status() != CompanyStatus.DISSOLVED)
                .count();
    }

    // --- creation ----------------------------------------------------------------------------

    /** Validation-only check for the player self-service path (mode, cooldown, limit, name, type). */
    public CompanyResult canCreateForPlayer(UUID ownerUuid, String displayName, String type) {
        if (!settings.creationMode().allowsDirectPlayerCreate()) {
            return CompanyResult.fail("creation.player_disabled", "mode", settings.creationMode().name());
        }
        long cooldownRemaining = cooldownRemainingMillis(ownerUuid);
        if (cooldownRemaining > 0) {
            return CompanyResult.fail("creation.cooldown", "seconds", (cooldownRemaining / 1000) + 1);
        }
        if (settings.maxOwnedPerPlayer() >= 0 && ownedCount(ownerUuid) >= settings.maxOwnedPerPlayer()) {
            return CompanyResult.fail("creation.limit_reached", "limit", settings.maxOwnedPerPlayer());
        }
        return validateCommon(displayName, type);
    }

    /** Full player self-service creation; on success the owner is CEO and the cooldown is armed. */
    public CompanyResult createForPlayer(UUID ownerUuid, String ownerName, String displayName, String type) {
        CompanyResult check = canCreateForPlayer(ownerUuid, displayName, type);
        if (check.failed()) {
            return check;
        }
        Company company = buildAndStore(ownerUuid, ownerName, displayName, type);
        if (ownerUuid != null) {
            creationCooldowns.put(ownerUuid, System.currentTimeMillis());
        }
        return CompanyResult.ok("company.created", "name", company.displayName(), "id", company.id())
                .withPayload(company);
    }

    /** Validation-only check for the admin/application path (name, type, uniqueness). */
    public CompanyResult canCreateAsAdmin(String displayName, String type) {
        return validateCommon(displayName, type);
    }

    /** Admin/application creation; skips mode, cooldown, limit and fee. The owner becomes CEO. */
    public CompanyResult createAsAdmin(UUID ownerUuid, String ownerName, String displayName, String type) {
        CompanyResult check = validateCommon(displayName, type);
        if (check.failed()) {
            return check;
        }
        Company company = buildAndStore(ownerUuid, ownerName, displayName, type);
        return CompanyResult.ok("company.created", "name", company.displayName(), "id", company.id())
                .withPayload(company);
    }

    /** Removes a company entirely. */
    public CompanyResult delete(String companyId) {
        Company company = companies.remove(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        storage().deleteCompany(company.id());
        // Drop any invites that pointed at this company.
        invites.values().removeIf(invite -> invite.companyId().equals(company.id()));
        return CompanyResult.ok("company.deleted", "name", company.displayName(), "id", company.id())
                .withPayload(company);
    }

    // --- membership --------------------------------------------------------------------------

    public CompanyResult invite(String companyId, UUID inviterUuid, UUID targetUuid, String targetName, CompanyRole role) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (!company.status().canOperate()) {
            return CompanyResult.fail("company.not_active", "status", company.status().name());
        }
        if (role == null || role == CompanyRole.CEO) {
            return CompanyResult.fail("role.cannot_assign_ceo");
        }
        if (inviterUuid != null) {
            Optional<CompanyMember> inviter = company.member(inviterUuid);
            if (inviter.isEmpty() || !inviter.get().role().grants(CompanyCapability.INVITE)) {
                return CompanyResult.fail("member.no_permission");
            }
            if (!inviter.get().role().outranks(role)) {
                return CompanyResult.fail("invite.role_too_high");
            }
        }
        if (company.isMember(targetUuid)) {
            return CompanyResult.fail("invite.already_member");
        }
        if (company.memberCount() >= settings.maxMembersPerCompany()) {
            return CompanyResult.fail("company.full", "limit", settings.maxMembersPerCompany());
        }
        invites.put(targetUuid, new PendingInvite(company.id(), role, inviterUuid, System.currentTimeMillis()));
        return CompanyResult.ok("invite.sent", "company", company.displayName(), "role", role.name());
    }

    public CompanyResult acceptInvite(UUID targetUuid, String targetName) {
        PendingInvite invite = invites.get(targetUuid);
        if (invite == null) {
            return CompanyResult.fail("invite.none");
        }
        Company company = companies.get(invite.companyId());
        if (company == null) {
            invites.remove(targetUuid);
            return CompanyResult.fail("invite.company_gone");
        }
        if (!company.status().canOperate()) {
            return CompanyResult.fail("company.not_active", "status", company.status().name());
        }
        if (company.isMember(targetUuid)) {
            invites.remove(targetUuid);
            return CompanyResult.fail("invite.already_member");
        }
        if (company.memberCount() >= settings.maxMembersPerCompany()) {
            return CompanyResult.fail("company.full", "limit", settings.maxMembersPerCompany());
        }
        CompanyMember member = new CompanyMember(company.id(), targetUuid, targetName, invite.role(), 0.0,
                System.currentTimeMillis());
        company.addMember(member);
        invites.remove(targetUuid);
        storage().saveCompany(company);
        return CompanyResult.ok("invite.accepted", "company", company.displayName(), "role", invite.role().name())
                .withPayload(member);
    }

    public CompanyResult denyInvite(UUID targetUuid) {
        PendingInvite invite = invites.remove(targetUuid);
        if (invite == null) {
            return CompanyResult.fail("invite.none");
        }
        return CompanyResult.ok("invite.denied", "company", invite.companyId());
    }

    public CompanyResult removeMember(String companyId, UUID actorUuid, UUID targetUuid) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        Optional<CompanyMember> target = company.member(targetUuid);
        if (target.isEmpty()) {
            return CompanyResult.fail("member.not_found");
        }
        if (targetUuid.equals(company.ownerUuid())) {
            return CompanyResult.fail("member.cannot_remove_owner");
        }
        boolean selfLeave = actorUuid != null && actorUuid.equals(targetUuid);
        if (!selfLeave && actorUuid != null) {
            Optional<CompanyMember> actor = company.member(actorUuid);
            if (actor.isEmpty() || !actor.get().role().grants(CompanyCapability.FIRE)) {
                return CompanyResult.fail("member.no_permission");
            }
            if (!actor.get().role().outranks(target.get().role())) {
                return CompanyResult.fail("member.cannot_fire_higher");
            }
        }
        CompanyMember removed = company.removeMember(targetUuid);
        storage().saveCompany(company);
        String key = selfLeave ? "member.left" : "member.fired";
        return CompanyResult.ok(key, "player", removed.playerName(), "company", company.displayName())
                .withPayload(removed);
    }

    public CompanyResult changeRole(String companyId, UUID actorUuid, UUID targetUuid, CompanyRole newRole) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (newRole == null) {
            return CompanyResult.fail("role.unknown");
        }
        if (newRole == CompanyRole.CEO) {
            return CompanyResult.fail("role.cannot_assign_ceo");
        }
        Optional<CompanyMember> target = company.member(targetUuid);
        if (target.isEmpty()) {
            return CompanyResult.fail("member.not_found");
        }
        if (targetUuid.equals(company.ownerUuid())) {
            return CompanyResult.fail("role.cannot_change_owner");
        }
        CompanyRole previousRole = target.get().role();
        if (previousRole == newRole) {
            return CompanyResult.fail("role.unchanged", "role", newRole.name());
        }
        if (actorUuid != null) {
            Optional<CompanyMember> actor = company.member(actorUuid);
            if (actor.isEmpty() || !actor.get().role().grants(CompanyCapability.CHANGE_ROLE)) {
                return CompanyResult.fail("member.no_permission");
            }
            if (!actor.get().role().outranks(previousRole) || !actor.get().role().outranks(newRole)) {
                return CompanyResult.fail("role.insufficient_rank");
            }
        }
        target.get().setRole(newRole);
        storage().saveCompany(company);
        return CompanyResult.ok("role.changed", "player", target.get().playerName(), "role", newRole.name())
                .withPayload(target.get());
    }

    // --- chamber: applications ---------------------------------------------------------------

    public CompanyResult submitApplication(UUID applicantUuid, String applicantName, String requestedName,
                                           String requestedType, String description) {
        if (!settings.creationMode().allowsApplication()) {
            return CompanyResult.fail("application.disabled", "mode", settings.creationMode().name());
        }
        CompanyResult name = validator.validateName(requestedName);
        if (name.failed()) {
            return name;
        }
        CompanyResult type = validator.validateType(requestedType);
        if (type.failed()) {
            return type;
        }
        if (isNameTaken(requestedName)) {
            return CompanyResult.fail("application.name_taken", "name", requestedName);
        }
        boolean alreadyPending = applications.values().stream()
                .anyMatch(application -> application.isPending() && application.applicantUuid().equals(applicantUuid));
        if (alreadyPending) {
            return CompanyResult.fail("application.already_pending");
        }
        CompanyApplication application = new CompanyApplication(UUID.randomUUID(), applicantUuid, applicantName,
                requestedName.trim(), requestedType.toLowerCase(Locale.ROOT), description, System.currentTimeMillis());
        applications.put(application.id(), application);
        storage().saveApplication(application);
        return CompanyResult.ok("application.submitted", "id", application.shortId(), "name", requestedName.trim())
                .withPayload(application);
    }

    public CompanyResult approveApplication(UUID applicationId) {
        CompanyApplication application = applications.get(applicationId);
        if (application == null) {
            return CompanyResult.fail("application.not_found");
        }
        if (!application.isPending()) {
            return CompanyResult.fail("application.not_pending");
        }
        CompanyResult created = createAsAdmin(application.applicantUuid(), application.applicantName(),
                application.requestedName(), application.requestedType());
        if (created.failed()) {
            return created;
        }
        application.setStatus(CompanyApplication.Status.APPROVED);
        application.setResolution("approved");
        storage().saveApplication(application);
        Company company = created.company().orElse(null);
        return CompanyResult.ok("application.approved", "name", application.requestedName(),
                "player", application.applicantName()).withPayload(company);
    }

    public CompanyResult denyApplication(UUID applicationId, String reason) {
        CompanyApplication application = applications.get(applicationId);
        if (application == null) {
            return CompanyResult.fail("application.not_found");
        }
        if (!application.isPending()) {
            return CompanyResult.fail("application.not_pending");
        }
        application.setStatus(CompanyApplication.Status.DENIED);
        application.setResolution(reason == null || reason.isBlank() ? "denied" : reason);
        storage().saveApplication(application);
        return CompanyResult.ok("application.denied", "name", application.requestedName())
                .withPayload(application);
    }

    /** Reverts an application to pending (used if a create event vetoes an approval). */
    public void reopenApplication(UUID applicationId) {
        CompanyApplication application = applications.get(applicationId);
        if (application != null) {
            application.setStatus(CompanyApplication.Status.PENDING);
            application.setResolution("");
            storage().saveApplication(application);
        }
    }

    public Collection<CompanyApplication> applications() {
        return new ArrayList<>(applications.values());
    }

    public Collection<CompanyApplication> pendingApplications() {
        return applications.values().stream().filter(CompanyApplication::isPending).toList();
    }

    public Optional<CompanyApplication> findApplication(UUID applicationId) {
        return Optional.ofNullable(applications.get(applicationId));
    }

    public Optional<CompanyApplication> findApplicationByShortId(String shortId) {
        if (shortId == null || shortId.isBlank()) {
            return Optional.empty();
        }
        String needle = shortId.trim().toLowerCase(Locale.ROOT);
        return applications.values().stream()
                .filter(application -> application.id().toString().toLowerCase(Locale.ROOT).startsWith(needle))
                .findFirst();
    }

    // --- chamber: licenses, status, headquarters ---------------------------------------------

    public CompanyResult grantLicense(String companyId, CompanyLicenseType type) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (type == null) {
            return CompanyResult.fail("license.unknown");
        }
        company.setLicense(type, CompanyLicenseStatus.GRANTED);
        storage().saveCompany(company);
        return CompanyResult.ok("license.granted", "license", type.key(), "company", company.displayName())
                .withPayload(company);
    }

    public CompanyResult revokeLicense(String companyId, CompanyLicenseType type) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (type == null) {
            return CompanyResult.fail("license.unknown");
        }
        if (company.licenseStatus(type) == CompanyLicenseStatus.NONE) {
            return CompanyResult.fail("license.not_held", "license", type.key());
        }
        company.setLicense(type, CompanyLicenseStatus.REVOKED);
        storage().saveCompany(company);
        return CompanyResult.ok("license.revoked", "license", type.key(), "company", company.displayName())
                .withPayload(company);
    }

    public CompanyResult setStatus(String companyId, CompanyStatus status) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (status == null) {
            return CompanyResult.fail("status.unknown");
        }
        if (company.status() == status) {
            return CompanyResult.fail("status.unchanged", "status", status.name());
        }
        company.setStatus(status);
        storage().saveCompany(company);
        return CompanyResult.ok("status.changed", "company", company.displayName(), "status", status.name())
                .withPayload(company);
    }

    public CompanyResult setHeadquarters(String companyId, Company.Headquarters headquarters) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        company.setHeadquarters(headquarters);
        storage().saveCompany(company);
        return CompanyResult.ok("hq.set", "company", company.displayName()).withPayload(company);
    }

    /**
     * Transfers ownership to another player, who becomes CEO (added as a member if needed); the
     * previous owner, if still a member, is demoted to DIRECTOR.
     */
    public CompanyResult setOwner(String companyId, UUID newOwnerUuid, String newOwnerName) {
        Company company = companies.get(normalizeId(companyId));
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (newOwnerUuid == null) {
            return CompanyResult.fail("member.not_found");
        }
        if (newOwnerUuid.equals(company.ownerUuid())) {
            return CompanyResult.fail("owner.unchanged");
        }
        UUID previousOwner = company.ownerUuid();
        if (previousOwner != null) {
            company.member(previousOwner).ifPresent(member -> member.setRole(CompanyRole.DIRECTOR));
        }
        CompanyMember member = company.member(newOwnerUuid).orElse(null);
        if (member == null) {
            member = new CompanyMember(company.id(), newOwnerUuid, newOwnerName, CompanyRole.CEO, 0.0,
                    System.currentTimeMillis());
            company.addMember(member);
        } else {
            member.setRole(CompanyRole.CEO);
            member.setPlayerName(newOwnerName);
        }
        company.setOwnerUuid(newOwnerUuid);
        storage().saveCompany(company);
        return CompanyResult.ok("owner.changed", "company", company.displayName(), "player", member.playerName())
                .withPayload(company);
    }

    /** Persists a company after an externally-applied mutation (e.g. balance change). */
    public void persist(Company company) {
        if (company != null) {
            companies.put(company.id(), company);
            storage().saveCompany(company);
        }
    }

    // --- internals ---------------------------------------------------------------------------

    private CompanyResult validateCommon(String displayName, String type) {
        CompanyResult name = validator.validateName(displayName);
        if (name.failed()) {
            return name;
        }
        CompanyResult typeResult = validator.validateType(type);
        if (typeResult.failed()) {
            return typeResult;
        }
        if (isNameTaken(displayName)) {
            return CompanyResult.fail("creation.name_taken", "name", displayName.trim());
        }
        return CompanyResult.ok("validation.ok");
    }

    private boolean isNameTaken(String displayName) {
        String slug = CompanyValidator.slugify(displayName);
        if (companies.containsKey(slug)) {
            return true;
        }
        String needle = displayName.trim();
        return companies.values().stream().anyMatch(company -> company.displayName().equalsIgnoreCase(needle));
    }

    private Company buildAndStore(UUID ownerUuid, String ownerName, String displayName, String type) {
        String slug = CompanyValidator.slugify(displayName);
        long now = System.currentTimeMillis();
        Company company = new Company(slug, displayName.trim(), type.toLowerCase(Locale.ROOT), ownerUuid, now);
        company.setStatus(CompanyStatus.ACTIVE);
        if (ownerUuid != null) {
            company.addMember(new CompanyMember(slug, ownerUuid, ownerName, CompanyRole.CEO, 0.0, now));
        }
        companies.put(slug, company);
        storage().saveCompany(company);
        return company;
    }

    private long cooldownRemainingMillis(UUID ownerUuid) {
        Long last = ownerUuid == null ? null : creationCooldowns.get(ownerUuid);
        if (last == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - last;
        long remaining = settings.creationCooldownMillis() - elapsed;
        return Math.max(0, remaining);
    }

    private static String normalizeId(String companyId) {
        return companyId == null ? "" : companyId.trim().toLowerCase(Locale.ROOT);
    }
}
