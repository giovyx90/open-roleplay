package dev.openrp.politics.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Internal-voting rules for a charge that acts as a collegiate body ({@code max_holders > 1}). A
 * quorum of members must take part and a majority of cast ballots must approve before the vote
 * carries. Durations are in hours and scaled by the global time scale at use.
 *
 * @param enabled       whether this charge votes internally on acts
 * @param quorum        fraction of holders (0..1) that must cast a ballot
 * @param majority      fraction of cast ballots (0..1) needed to approve
 * @param durationHours how long the internal vote stays open
 */
public record CollegiateConfig(boolean enabled, double quorum, double majority, double durationHours) {

    public CollegiateConfig {
        quorum = clamp(quorum, 0.5);
        majority = clamp(majority, 0.5);
        durationHours = durationHours <= 0 ? 48 : durationHours;
    }

    public static CollegiateConfig from(ConfigurationSection section) {
        if (section == null) {
            return new CollegiateConfig(false, 0.5, 0.5, 48);
        }
        return new CollegiateConfig(
                section.getBoolean("enabled", false),
                section.getDouble("quorum", 0.5),
                section.getDouble("majority", 0.5),
                section.getDouble("duration_hours", 48));
    }

    private static double clamp(double value, double fallback) {
        if (value <= 0 || value > 1) {
            return fallback;
        }
        return value;
    }
}
