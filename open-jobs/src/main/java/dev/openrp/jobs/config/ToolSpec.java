package dev.openrp.jobs.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Optional tool requirement. When enabled, a session needs the minimum tool in the inventory; better
 * tools listed in {@code bonus_materials} grant a pay multiplier - an incentive to invest in good
 * gear. The plugin never consumes the tool; Minecraft handles durability normally.
 */
public final class ToolSpec {

    private final boolean enabled;
    private final String material;
    private final Map<String, Double> bonusMaterials;

    public ToolSpec(boolean enabled, String material, Map<String, Double> bonusMaterials) {
        this.enabled = enabled;
        this.material = material;
        this.bonusMaterials = bonusMaterials;
    }

    public static ToolSpec disabled() {
        return new ToolSpec(false, "", Map.of());
    }

    public static ToolSpec from(ConfigurationSection section) {
        if (section == null) {
            return disabled();
        }
        Map<String, Double> bonus = new LinkedHashMap<>();
        ConfigurationSection bonusSection = section.getConfigurationSection("bonus_materials");
        if (bonusSection != null) {
            for (String key : bonusSection.getKeys(false)) {
                bonus.put(key.toUpperCase(Locale.ROOT), bonusSection.getDouble(key, 1.0));
            }
        }
        return new ToolSpec(
                section.getBoolean("enabled", false),
                section.getString("material", ""),
                bonus);
    }

    public boolean enabled() {
        return enabled;
    }

    public String material() {
        return material;
    }

    public Map<String, Double> bonusMaterials() {
        return bonusMaterials;
    }

    /** The pay multiplier for holding {@code heldMaterial} (Bukkit material name), or 1.0 if no bonus applies. */
    public double bonusFor(String heldMaterial) {
        if (heldMaterial == null) {
            return 1.0;
        }
        return bonusMaterials.getOrDefault(heldMaterial.toUpperCase(Locale.ROOT), 1.0);
    }
}
