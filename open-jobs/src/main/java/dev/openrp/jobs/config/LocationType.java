package dev.openrp.jobs.config;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.jobs.model.ActivityDetection;

/**
 * A kind of place where work happens (mine, forest, farm, water, workshop, ...), defining how activity
 * is detected and which materials count. Fully config-driven and setting-neutral: the core has no idea
 * what a "mine" is, only that a location of this type counts these actions on these materials.
 */
public final class LocationType {

    private final String id;
    private final String displayName;
    private final ActivityDetection activityDetection;
    private final Set<String> validMaterials;
    private final String regionTag;

    public LocationType(String id, String displayName, ActivityDetection activityDetection,
                        Set<String> validMaterials, String regionTag) {
        this.id = id;
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        this.activityDetection = activityDetection;
        this.validMaterials = validMaterials;
        this.regionTag = regionTag;
    }

    public static LocationType from(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Set<String> materials = new HashSet<>();
        for (String material : section.getStringList("valid_materials")) {
            if (material != null && !material.isBlank()) {
                materials.add(material.trim().toUpperCase(Locale.ROOT));
            }
        }
        return new LocationType(
                id,
                section.getString("display_name", id),
                ActivityDetection.fromString(section.getString("activity_detection")),
                materials,
                section.getString("region_tag", "job_" + id));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ActivityDetection activityDetection() {
        return activityDetection;
    }

    public String regionTag() {
        return regionTag;
    }

    /** Whether a material counts as work here. With no list configured, fishing/crafting types accept anything caught/crafted; block types accept nothing (so a typo is visible, not silently rewarded). */
    public boolean accepts(String material) {
        if (validMaterials.isEmpty()) {
            return activityDetection == ActivityDetection.FISHING || activityDetection == ActivityDetection.CRAFTING;
        }
        return material != null && validMaterials.contains(material.toUpperCase(Locale.ROOT));
    }

    public Set<String> validMaterials() {
        return validMaterials;
    }
}
