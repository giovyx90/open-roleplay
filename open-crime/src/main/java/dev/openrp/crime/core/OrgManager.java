package dev.openrp.crime.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.Hierarchy;
import dev.openrp.crime.config.OrgRank;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.OrgMember;
import dev.openrp.crime.model.OrgStatus;

/**
 * Owns the organisations: founding, membership, ranks and capability resolution. The hierarchy is
 * entirely config-driven, so the manager never hardcodes a rank name - it asks the {@code Hierarchy}
 * for the default, next and apical ranks. A player belongs to at most one org, indexed for O(1)
 * lookup. All mutating methods are synchronized so two concurrent commands cannot race.
 */
public final class OrgManager {

    private static final long INVITE_TTL_MILLIS = 120_000L;

    private final Hierarchy hierarchy;
    private final AdapterRegistry adapters;
    // ConcurrentHashMap so the unsynchronized public-API reads (get/byMember/all) are safe even if an
    // integration calls them off the main thread while a command mutates the roster.
    private final Map<String, IllegalOrg> orgs = new ConcurrentHashMap<>();
    private final Map<UUID, String> memberIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();

    public OrgManager(Hierarchy hierarchy, AdapterRegistry adapters) {
        this.hierarchy = hierarchy;
        this.adapters = adapters;
    }

    public synchronized void loadAll() {
        orgs.clear();
        memberIndex.clear();
        for (IllegalOrg org : adapters.storage().loadOrgs()) {
            orgs.put(org.id(), org);
            // Only ACTIVE orgs index their members. A dissolved org keeps its roster for history, but
            // its members must stay free to found/join elsewhere - otherwise a reload would re-trap them.
            if (org.isActive()) {
                for (OrgMember member : org.members()) {
                    memberIndex.put(member.uuid(), org.id());
                }
            }
        }
    }

    // --- lookups -----------------------------------------------------------------------------

    public Optional<IllegalOrg> get(String orgId) {
        return Optional.ofNullable(orgId == null ? null : orgs.get(orgId));
    }

    public Optional<IllegalOrg> byMember(UUID player) {
        String orgId = player == null ? null : memberIndex.get(player);
        return get(orgId);
    }

    public boolean isMember(UUID player, String orgId) {
        return orgId != null && orgId.equals(memberIndex.get(player));
    }

    public Optional<OrgMember> memberOf(UUID player) {
        return byMember(player).flatMap(org -> org.member(player));
    }

    public Optional<OrgRank> roleOf(UUID player) {
        return memberOf(player).flatMap(member -> hierarchy.rank(member.roleId()));
    }

    /**
     * Whether the player holds the capability. Capabilities are inherited upward: a rank holds every
     * capability granted to any rank at or below its order, plus the {@link Capability#ALL} wildcard.
     */
    public boolean has(UUID player, Capability capability) {
        OrgRank role = roleOf(player).orElse(null);
        if (role == null) {
            return false;
        }
        if (role.has(Capability.ALL)) {
            return true;
        }
        for (OrgRank rank : hierarchy.ranks()) {
            if (rank.order() <= role.order() && rank.has(capability)) {
                return true;
            }
        }
        return false;
    }

    public boolean isApical(UUID player) {
        return roleOf(player).map(OrgRank::apical).orElse(false);
    }

    public Collection<IllegalOrg> all() {
        return Collections.unmodifiableCollection(orgs.values());
    }

    public boolean nameExists(String name) {
        if (name == null) {
            return false;
        }
        for (IllegalOrg org : orgs.values()) {
            if (org.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // --- founding ----------------------------------------------------------------------------

    /**
     * Founds a new organisation. {@code crew} are nearby players pooled as initial members (consent is
     * by physical presence); the founder takes the apical rank. Enforces the configured minimum member
     * count, name uniqueness and that no one involved already belongs to an org.
     */
    public synchronized CrimeResult found(UUID founder, String founderName, String name, String type,
                                          Map<UUID, String> crew) {
        if (name == null || name.isBlank()) {
            return CrimeResult.fail("syndicate.name_required");
        }
        if (nameExists(name)) {
            return CrimeResult.fail("syndicate.name_taken", "name", name);
        }
        if (memberIndex.containsKey(founder)) {
            return CrimeResult.fail("syndicate.already_member");
        }
        Map<UUID, String> safeCrew = crew == null ? Map.of() : crew;
        for (UUID member : safeCrew.keySet()) {
            if (memberIndex.containsKey(member) || member.equals(founder)) {
                return CrimeResult.fail("syndicate.crew_busy");
            }
        }
        int total = 1 + safeCrew.size();
        int required = hierarchy.minMembers();
        if (total < required) {
            return CrimeResult.fail("syndicate.need_members", "required", required, "have", total);
        }
        Optional<OrgRank> apical = hierarchy.apicalRank();
        Optional<OrgRank> base = hierarchy.defaultRank();
        if (apical.isEmpty() || base.isEmpty()) {
            return CrimeResult.fail("syndicate.no_hierarchy");
        }

        String id = uniqueId(name);
        IllegalOrg org = new IllegalOrg(id, name, type == null ? "" : type, founder,
                System.currentTimeMillis(), UUID.randomUUID());
        org.addMember(founder, founderName, apical.get().id(), System.currentTimeMillis());
        for (Map.Entry<UUID, String> entry : safeCrew.entrySet()) {
            org.addMember(entry.getKey(), entry.getValue(), base.get().id(), System.currentTimeMillis());
        }
        orgs.put(id, org);
        memberIndex.put(founder, id);
        for (UUID member : safeCrew.keySet()) {
            memberIndex.put(member, id);
        }
        adapters.storage().saveOrg(org);
        return CrimeResult.ok("syndicate.founded", "name", name, "id", id).withPayload(org);
    }

    // --- recruitment -------------------------------------------------------------------------

    public synchronized CrimeResult invite(UUID inviter, UUID invitee, String inviteeName) {
        IllegalOrg org = byMember(inviter).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!has(inviter, Capability.INVITE)) {
            return CrimeResult.fail("general.no_capability");
        }
        if (invitee == null) {
            return CrimeResult.fail("general.player_not_found");
        }
        if (memberIndex.containsKey(invitee)) {
            return CrimeResult.fail("syndicate.target_busy");
        }
        invites.put(invitee, new Invite(org.id(), System.currentTimeMillis() + INVITE_TTL_MILLIS));
        return CrimeResult.ok("syndicate.invited", "name", inviteeName == null ? "" : inviteeName,
                "org", org.name()).withPayload(org);
    }

    public synchronized CrimeResult accept(UUID invitee, String inviteeName) {
        Invite invite = invites.get(invitee);
        if (invite == null || invite.expiresAt() < System.currentTimeMillis()) {
            invites.remove(invitee);
            return CrimeResult.fail("syndicate.no_invite");
        }
        if (memberIndex.containsKey(invitee)) {
            return CrimeResult.fail("syndicate.already_member");
        }
        IllegalOrg org = orgs.get(invite.orgId());
        if (org == null || !org.isActive()) {
            invites.remove(invitee);
            return CrimeResult.fail("syndicate.org_gone");
        }
        Optional<OrgRank> base = hierarchy.defaultRank();
        if (base.isEmpty()) {
            return CrimeResult.fail("syndicate.no_hierarchy");
        }
        org.addMember(invitee, inviteeName, base.get().id(), System.currentTimeMillis());
        memberIndex.put(invitee, org.id());
        invites.remove(invitee);
        adapters.storage().saveOrg(org);
        return CrimeResult.ok("syndicate.joined", "org", org.name()).withPayload(org);
    }

    // --- management --------------------------------------------------------------------------

    public synchronized CrimeResult expel(UUID actor, UUID target) {
        IllegalOrg org = byMember(actor).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!has(actor, Capability.EXPEL)) {
            return CrimeResult.fail("general.no_capability");
        }
        if (!org.isMember(target)) {
            return CrimeResult.fail("syndicate.not_a_member");
        }
        if (target.equals(org.founder())) {
            return CrimeResult.fail("syndicate.cannot_expel_founder");
        }
        if (target.equals(actor)) {
            return CrimeResult.fail("syndicate.cannot_expel_self");
        }
        org.removeMember(target);
        memberIndex.remove(target);
        adapters.storage().saveOrg(org);
        return CrimeResult.ok("syndicate.expelled").withPayload(org);
    }

    public synchronized CrimeResult promote(UUID actor, UUID target, boolean up) {
        IllegalOrg org = byMember(actor).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!has(actor, up ? Capability.PROMOTE : Capability.DEMOTE)) {
            return CrimeResult.fail("general.no_capability");
        }
        OrgMember member = org.member(target).orElse(null);
        if (member == null) {
            return CrimeResult.fail("syndicate.not_a_member");
        }
        if (target.equals(actor)) {
            return CrimeResult.fail("syndicate.cannot_rank_self");
        }
        Optional<OrgRank> destination = up
                ? hierarchy.nextRank(member.roleId())
                : hierarchy.previousRank(member.roleId());
        if (destination.isEmpty()) {
            return CrimeResult.fail(up ? "syndicate.already_top" : "syndicate.already_bottom");
        }
        // Never let a non-apical actor mint an apical equal/superior to themselves.
        if (destination.get().apical() && !isApical(actor)) {
            return CrimeResult.fail("general.no_capability");
        }
        member.setRoleId(destination.get().id());
        adapters.storage().saveOrg(org);
        return CrimeResult.ok(up ? "syndicate.promoted" : "syndicate.demoted",
                "rank", destination.get().displayName()).withPayload(org);
    }

    public synchronized CrimeResult dissolve(UUID actor) {
        IllegalOrg org = byMember(actor).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!has(actor, Capability.DISSOLVE)) {
            return CrimeResult.fail("general.no_capability");
        }
        org.setStatus(OrgStatus.DISSOLVED);
        for (OrgMember member : org.members()) {
            memberIndex.remove(member.uuid());
        }
        adapters.storage().saveOrg(org);
        return CrimeResult.ok("syndicate.dissolved", "name", org.name()).withPayload(org);
    }

    /** Marks an org as under investigation. Called by the discovery layer, never automatically. */
    public synchronized void markInvestigated(String orgId) {
        IllegalOrg org = orgs.get(orgId);
        if (org != null && org.status() == OrgStatus.ACTIVE) {
            org.setStatus(OrgStatus.INVESTIGATED);
            adapters.storage().saveOrg(org);
        }
    }

    /** Marks a member as an informant (collaborator). Narrative consequences are left to RP. */
    public synchronized void markInformant(String orgId, UUID member) {
        IllegalOrg org = orgs.get(orgId);
        if (org == null) {
            return;
        }
        org.member(member).ifPresent(record -> {
            record.setInformant(true);
            adapters.storage().saveOrg(org);
        });
    }

    private String uniqueId(String name) {
        String base = Ids.slug(name);
        String id = base;
        int suffix = 2;
        while (orgs.containsKey(id)) {
            id = base + "_" + suffix++;
        }
        return id;
    }

    private record Invite(String orgId, long expiresAt) {
    }
}
