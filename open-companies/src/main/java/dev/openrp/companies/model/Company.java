package dev.openrp.companies.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root for a single company. Holds identity, ownership, status, an optional demo balance,
 * its members, its license book and an optional headquarters. Intentionally free of Bukkit types: a
 * {@link Headquarters} stores plain coordinates, and the command/region layers convert to and from a
 * real {@code Location} at the boundary. This keeps the model unit-testable and storage-agnostic.
 *
 * <p>This class is not thread-safe on its own; callers mutate a company only while holding the
 * relevant company lock (see {@code CompanyLocks}).</p>
 */
public final class Company {

    private final String id;
    private String displayName;
    private String type;
    private UUID ownerUuid;
    private CompanyStatus status = CompanyStatus.ACTIVE;
    private double balance;
    private final long createdAt;
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private final Map<UUID, CompanyMember> members = new LinkedHashMap<>();
    private final Map<CompanyLicenseType, CompanyLicenseStatus> licenses = new EnumMap<>(CompanyLicenseType.class);
    private Headquarters headquarters;

    public Company(String id, String displayName, String type, UUID ownerUuid, long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? id : displayName;
        this.type = type == null ? "generic" : type;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    public String type() {
        return type;
    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = type;
        }
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public CompanyStatus status() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    // --- members -----------------------------------------------------------------------------

    /** Live, unmodifiable view of the members. */
    public Collection<CompanyMember> members() {
        return Collections.unmodifiableCollection(members.values());
    }

    public Optional<CompanyMember> member(UUID playerUuid) {
        return Optional.ofNullable(members.get(playerUuid));
    }

    public boolean isMember(UUID playerUuid) {
        return members.containsKey(playerUuid);
    }

    public void addMember(CompanyMember member) {
        members.put(member.playerUuid(), member);
    }

    public CompanyMember removeMember(UUID playerUuid) {
        return members.remove(playerUuid);
    }

    public int memberCount() {
        return members.size();
    }

    /** The owning member (matched by {@link #ownerUuid()}), if present in the roster. */
    public Optional<CompanyMember> owner() {
        return ownerUuid == null ? Optional.empty() : member(ownerUuid);
    }

    // --- licenses ----------------------------------------------------------------------------

    /** Live, unmodifiable view of stored license statuses. */
    public Map<CompanyLicenseType, CompanyLicenseStatus> licenses() {
        return Collections.unmodifiableMap(licenses);
    }

    public CompanyLicenseStatus licenseStatus(CompanyLicenseType type) {
        return licenses.getOrDefault(type, CompanyLicenseStatus.NONE);
    }

    public boolean hasLicense(CompanyLicenseType type) {
        return licenseStatus(type).isActive();
    }

    public void setLicense(CompanyLicenseType type, CompanyLicenseStatus status) {
        if (type == null) {
            return;
        }
        if (status == null || status == CompanyLicenseStatus.NONE) {
            licenses.remove(type);
        } else {
            licenses.put(type, status);
        }
    }

    // --- headquarters ------------------------------------------------------------------------

    public Optional<Headquarters> headquarters() {
        return Optional.ofNullable(headquarters);
    }

    public void setHeadquarters(Headquarters headquarters) {
        this.headquarters = headquarters;
    }

    /** Plain-coordinate headquarters; converted to/from a Bukkit {@code Location} at the boundary. */
    public record Headquarters(String world, double x, double y, double z, float yaw, float pitch) {
    }
}
