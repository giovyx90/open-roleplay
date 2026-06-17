package dev.openrp.jobs.config;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.jobs.model.Season;

/**
 * Optional seasonal pay multiplier. The farmer earns more at harvest, the fisher in summer, the
 * woodcutter before winter - narrative economics with no complex machinery, just a per-season
 * coefficient applied to the payout.
 */
public final class SeasonalSpec {

    private final boolean enabled;
    private final Map<Season, Double> multipliers;

    public SeasonalSpec(boolean enabled, Map<Season, Double> multipliers) {
        this.enabled = enabled;
        this.multipliers = multipliers;
    }

    public static SeasonalSpec disabled() {
        return new SeasonalSpec(false, Map.of());
    }

    public static SeasonalSpec from(ConfigurationSection section) {
        if (section == null) {
            return disabled();
        }
        Map<Season, Double> multipliers = new EnumMap<>(Season.class);
        for (Season season : Season.values()) {
            if (section.isSet(season.configKey())) {
                multipliers.put(season, Math.max(0.0, section.getDouble(season.configKey(), 1.0)));
            }
        }
        return new SeasonalSpec(section.getBoolean("enabled", false), multipliers);
    }

    public boolean enabled() {
        return enabled;
    }

    public double multiplierFor(Season season) {
        if (!enabled || season == null) {
            return 1.0;
        }
        return multipliers.getOrDefault(season, 1.0);
    }
}
