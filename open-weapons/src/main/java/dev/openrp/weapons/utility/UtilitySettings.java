package dev.openrp.weapons.utility;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record UtilitySettings(
        int scannerDurationSeconds,
        int handcuffDurationSeconds,
        int restraintDurationSeconds,
        int gagUses,
        int blindfoldUses,
        int fireAxeCooldownSeconds,
        int fireAxeUses,
        Material fireAxeMaterial,
        int fireAxeDoorOpenSeconds,
        double grapplingHookMaxDistance,
        int grapplingHookCooldownSeconds,
        boolean trackerShowCoordinates,
        boolean trackerShowRegion,
        double statusTagYOffset
) {
    private static final int DEFAULT_SCANNER_DURATION_SECONDS = 3;
    private static final int DEFAULT_HANDCUFF_DURATION_SECONDS = 5;
    private static final int DEFAULT_RESTRAINT_DURATION_SECONDS = 3;
    private static final int DEFAULT_GAG_USES = 5;
    private static final int DEFAULT_BLINDFOLD_USES = 5;
    private static final int DEFAULT_FIRE_AXE_COOLDOWN_SECONDS = 5;
    private static final int DEFAULT_FIRE_AXE_USES = 131;
    private static final Material DEFAULT_FIRE_AXE_MATERIAL = Material.PAPER;
    private static final int DEFAULT_FIRE_AXE_DOOR_OPEN_SECONDS = 5;
    private static final double DEFAULT_GRAPPLING_HOOK_MAX_DISTANCE = 20.0D;
    private static final int DEFAULT_GRAPPLING_HOOK_COOLDOWN_SECONDS = 3;
    private static final boolean DEFAULT_TRACKER_SHOW_COORDINATES = false;
    private static final boolean DEFAULT_TRACKER_SHOW_REGION = true;
    private static final double DEFAULT_STATUS_TAG_Y_OFFSET = 2.9D;

    public static UtilitySettings defaults() {
        return new UtilitySettings(
                DEFAULT_SCANNER_DURATION_SECONDS,
                DEFAULT_HANDCUFF_DURATION_SECONDS,
                DEFAULT_RESTRAINT_DURATION_SECONDS,
                DEFAULT_GAG_USES,
                DEFAULT_BLINDFOLD_USES,
                DEFAULT_FIRE_AXE_COOLDOWN_SECONDS,
                DEFAULT_FIRE_AXE_USES,
                DEFAULT_FIRE_AXE_MATERIAL,
                DEFAULT_FIRE_AXE_DOOR_OPEN_SECONDS,
                DEFAULT_GRAPPLING_HOOK_MAX_DISTANCE,
                DEFAULT_GRAPPLING_HOOK_COOLDOWN_SECONDS,
                DEFAULT_TRACKER_SHOW_COORDINATES,
                DEFAULT_TRACKER_SHOW_REGION,
                DEFAULT_STATUS_TAG_Y_OFFSET
        );
    }

    public static UtilitySettings fromConfig(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        return new UtilitySettings(
                positive(section.getInt("scanner-duration-seconds", DEFAULT_SCANNER_DURATION_SECONDS), DEFAULT_SCANNER_DURATION_SECONDS),
                positive(section.getInt("handcuff-duration-seconds", DEFAULT_HANDCUFF_DURATION_SECONDS), DEFAULT_HANDCUFF_DURATION_SECONDS),
                positive(section.getInt("restraint-duration-seconds", DEFAULT_RESTRAINT_DURATION_SECONDS), DEFAULT_RESTRAINT_DURATION_SECONDS),
                positive(section.getInt("gag-uses", DEFAULT_GAG_USES), DEFAULT_GAG_USES),
                positive(section.getInt("blindfold-uses", DEFAULT_BLINDFOLD_USES), DEFAULT_BLINDFOLD_USES),
                positive(section.getInt("fire-axe-cooldown-seconds", DEFAULT_FIRE_AXE_COOLDOWN_SECONDS), DEFAULT_FIRE_AXE_COOLDOWN_SECONDS),
                positive(section.getInt("fire-axe-uses", DEFAULT_FIRE_AXE_USES), DEFAULT_FIRE_AXE_USES),
                material(section.getString("fire-axe-material", DEFAULT_FIRE_AXE_MATERIAL.name()), DEFAULT_FIRE_AXE_MATERIAL),
                positive(section.getInt("fire-axe-door-open-seconds", DEFAULT_FIRE_AXE_DOOR_OPEN_SECONDS), DEFAULT_FIRE_AXE_DOOR_OPEN_SECONDS),
                positive(section.getDouble("grappling-hook-max-distance", DEFAULT_GRAPPLING_HOOK_MAX_DISTANCE), DEFAULT_GRAPPLING_HOOK_MAX_DISTANCE),
                positive(section.getInt("grappling-hook-cooldown-seconds", DEFAULT_GRAPPLING_HOOK_COOLDOWN_SECONDS), DEFAULT_GRAPPLING_HOOK_COOLDOWN_SECONDS),
                section.getBoolean("tracker-show-coordinates", DEFAULT_TRACKER_SHOW_COORDINATES),
                section.getBoolean("tracker-show-region", DEFAULT_TRACKER_SHOW_REGION),
                positive(section.getDouble("status-tag-y-offset", DEFAULT_STATUS_TAG_Y_OFFSET), DEFAULT_STATUS_TAG_Y_OFFSET)
        );
    }

    public long scannerDurationMillis() {
        return scannerDurationSeconds * 1000L;
    }

    public long handcuffDurationMillis() {
        return handcuffDurationSeconds * 1000L;
    }

    public long restraintDurationMillis() {
        return restraintDurationSeconds * 1000L;
    }

    public long fireAxeCooldownMillis() {
        return fireAxeCooldownSeconds * 1000L;
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static double positive(double value, double fallback) {
        return value > 0.0D ? value : fallback;
    }

    private static Material material(String value, Material fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.trim());
        return material == null || material.isAir() ? fallback : material;
    }
}
