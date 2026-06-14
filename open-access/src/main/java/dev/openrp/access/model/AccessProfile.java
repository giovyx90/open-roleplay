package dev.openrp.access.model;

import org.bukkit.Location;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class AccessProfile {

    private final String id;
    private final AccessProfileType type;
    private final String world;
    private final String regionId;
    private final UUID ownerUuid;
    private final String ownerName;
    private final String ownerKey;
    private final String displayName;
    private final AccessPreset defaultPreset;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AccessProfile(String id, AccessProfileType type, String world, String regionId,
                         UUID ownerUuid, String ownerName, String ownerKey, String displayName,
                         AccessPreset defaultPreset, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id == null || id.isBlank() ? "access-" + UUID.randomUUID() : id;
        this.type = type == null ? AccessProfileType.REGION : type;
        this.world = world;
        this.regionId = regionId;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.ownerKey = ownerKey;
        this.displayName = displayName == null || displayName.isBlank() ? regionId : displayName;
        this.defaultPreset = defaultPreset == null ? AccessPreset.PRIVATE : defaultPreset;
        this.enabled = enabled;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static String regionKey(String world, String regionId) {
        return (world == null ? "" : world.trim().toLowerCase(Locale.ROOT))
                + ":"
                + (regionId == null ? "" : regionId.trim().toLowerCase(Locale.ROOT));
    }

    public String regionKey() {
        return regionKey(world, regionId);
    }

    public boolean isOwner(UUID uuid) {
        return uuid != null && ownerUuid != null && ownerUuid.equals(uuid);
    }

    public boolean isAtWorld(Location location) {
        return location != null
                && location.getWorld() != null
                && world != null
                && world.equalsIgnoreCase(location.getWorld().getName());
    }

    public AccessProfile withDefaultPreset(AccessPreset preset) {
        return new AccessProfile(id, type, world, regionId, ownerUuid, ownerName, ownerKey,
                displayName, preset == null ? defaultPreset : preset, enabled, createdAt, Instant.now());
    }

    public String getId() {
        return id;
    }

    public AccessProfileType getType() {
        return type;
    }

    public String getWorld() {
        return world;
    }

    public String getRegionId() {
        return regionId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AccessPreset getDefaultPreset() {
        return defaultPreset;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
