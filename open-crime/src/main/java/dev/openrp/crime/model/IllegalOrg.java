package dev.openrp.crime.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An illegal organisation. The core knows it has an identity, a configurable {@code type}, a founder,
 * a roster of {@link OrgMember members}, a list of controlled territory region ids and a treasury id
 * for the (optional) economy adapter. It has no "heat level": its history lives in {@link CrimeEvent}
 * records that the authorities can only reconstruct through {@link Discovery} events.
 *
 * <p>Bukkit-free and mutated only under the org lock held by the owning manager.</p>
 */
public final class IllegalOrg {

    private final String id;
    private String name;
    private final String type;
    private OrgStatus status = OrgStatus.ACTIVE;
    private final UUID founder;
    private final long createdAt;
    private final UUID treasury;
    private final Map<UUID, OrgMember> members = new LinkedHashMap<>();
    private final List<String> territories = new ArrayList<>();

    public IllegalOrg(String id, String name, String type, UUID founder, long createdAt, UUID treasury) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = name == null ? "" : name;
        this.type = type == null ? "" : type;
        this.founder = founder;
        this.createdAt = createdAt;
        this.treasury = treasury == null ? UUID.randomUUID() : treasury;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String type() {
        return type;
    }

    public OrgStatus status() {
        return status;
    }

    public void setStatus(OrgStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isActive() {
        return status == OrgStatus.ACTIVE;
    }

    public UUID founder() {
        return founder;
    }

    public long createdAt() {
        return createdAt;
    }

    public UUID treasury() {
        return treasury;
    }

    // --- members -----------------------------------------------------------------------------

    public List<OrgMember> members() {
        return List.copyOf(members.values());
    }

    public Optional<OrgMember> member(UUID uuid) {
        return Optional.ofNullable(uuid == null ? null : members.get(uuid));
    }

    public boolean isMember(UUID uuid) {
        return uuid != null && members.containsKey(uuid);
    }

    public int memberCount() {
        return members.size();
    }

    public OrgMember addMember(UUID uuid, String name, String roleId, long joinedAt) {
        OrgMember member = new OrgMember(uuid, name, roleId, joinedAt);
        members.put(uuid, member);
        return member;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    // --- territory ---------------------------------------------------------------------------

    public List<String> territories() {
        return Collections.unmodifiableList(territories);
    }

    public boolean controls(String regionId) {
        return regionId != null && territories.contains(regionId);
    }

    public void addTerritory(String regionId) {
        if (regionId != null && !territories.contains(regionId)) {
            territories.add(regionId);
        }
    }

    public void removeTerritory(String regionId) {
        territories.remove(regionId);
    }
}
