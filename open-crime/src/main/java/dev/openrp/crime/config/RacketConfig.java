package dev.openrp.crime.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Racket defaults and escalation levels, loaded from {@code racket.yml}. */
public final class RacketConfig {

    private final Map<Integer, EscalationLevel> levels = new LinkedHashMap<>();
    private long defaultAmount;
    private int defaultPeriodDays = 7;

    public void load(ConfigurationSection root) {
        levels.clear();
        defaultAmount = 0L;
        defaultPeriodDays = 7;
        if (root == null) {
            return;
        }
        ConfigurationSection defaults = root.getConfigurationSection("defaults");
        if (defaults != null) {
            defaultAmount = Math.max(0L, defaults.getLong("amount", 0L));
            defaultPeriodDays = Math.max(1, defaults.getInt("period_days", 7));
        }
        ConfigurationSection escalation = root.getConfigurationSection("escalation");
        ConfigurationSection levelSection = escalation == null ? null : escalation.getConfigurationSection("levels");
        if (levelSection != null) {
            for (String key : levelSection.getKeys(false)) {
                ConfigurationSection level = levelSection.getConfigurationSection(key);
                int number;
                try {
                    number = Integer.parseInt(key.trim());
                } catch (NumberFormatException invalid) {
                    continue;
                }
                if (level == null) {
                    continue;
                }
                levels.put(number, new EscalationLevel(
                        number,
                        level.getString("name", "Livello " + number),
                        level.getString("effect", "none"),
                        level.getInt("reputation_malus", 0)));
            }
        }
    }

    public Optional<EscalationLevel> level(int number) {
        return Optional.ofNullable(levels.get(number));
    }

    public Map<Integer, EscalationLevel> levels() {
        return Collections.unmodifiableMap(levels);
    }

    public int maxLevel() {
        return levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public long defaultAmount() {
        return defaultAmount;
    }

    public int defaultPeriodDays() {
        return defaultPeriodDays;
    }
}
