package dev.openrp.jobs.config;

import org.bukkit.configuration.ConfigurationSection;

/** A material id and a count, used by transformation inputs and outputs. The material is kept as the raw config string and resolved to a Bukkit material only at use time, so a typo surfaces as a clear error. */
public record ItemAmount(String material, int amount) {

    public static ItemAmount from(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String material = section.getString("material");
        if (material == null || material.isBlank()) {
            return null;
        }
        return new ItemAmount(material.trim(), Math.max(1, section.getInt("amount", 1)));
    }
}
