package dev.openrp.jobs.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The configured seniority ladder and its optional decay. The core hardcodes no tier - number and
 * thresholds come from {@code progression.yml}. Decay slowly erodes the effective session count of a
 * worker who disappears, so a long-absent "master" does not keep the bonus forever.
 */
public final class ProgressionLadder {

    private final List<ProgressionTier> tiers = new ArrayList<>();
    private boolean decayEnabled;
    private int inactiveDaysThreshold = 30;
    private double decaySessionsPerDay = 0.5;

    public void load(ConfigurationSection root) {
        tiers.clear();
        decayEnabled = false;
        inactiveDaysThreshold = 30;
        decaySessionsPerDay = 0.5;
        if (root == null) {
            return;
        }
        for (Map<?, ?> raw : root.getMapList("tiers")) {
            String id = asString(raw.get("id"));
            if (id == null) {
                continue;
            }
            tiers.add(new ProgressionTier(id,
                    asInt(raw.get("order"), tiers.size()),
                    asInt(raw.get("sessions_required"), 0),
                    asDouble(raw.get("pay_multiplier"), 1.0),
                    asString(raw.get("display_name"))));
        }
        tiers.sort(Comparator.comparingInt(ProgressionTier::sessionsRequired)
                .thenComparingInt(ProgressionTier::order));

        ConfigurationSection decay = root.getConfigurationSection("decay");
        if (decay != null) {
            decayEnabled = decay.getBoolean("enabled", false);
            inactiveDaysThreshold = Math.max(1, decay.getInt("inactive_days_threshold", 30));
            decaySessionsPerDay = Math.max(0.0, decay.getDouble("decay_sessions_per_day", 0.5));
        }
    }

    public boolean isEmpty() {
        return tiers.isEmpty();
    }

    public List<ProgressionTier> tiers() {
        return tiers;
    }

    /** The highest tier whose threshold is met by {@code effectiveSessions}, or the lowest tier as a floor. */
    public Optional<ProgressionTier> tierFor(double effectiveSessions) {
        ProgressionTier match = null;
        for (ProgressionTier tier : tiers) {
            if (effectiveSessions >= tier.sessionsRequired()) {
                match = tier;
            }
        }
        if (match == null && !tiers.isEmpty()) {
            match = tiers.get(0);
        }
        return Optional.ofNullable(match);
    }

    public Optional<ProgressionTier> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return tiers.stream().filter(tier -> tier.id().equals(id)).findFirst();
    }

    public double multiplierFor(String tierId) {
        return byId(tierId).map(ProgressionTier::payMultiplier).orElse(1.0);
    }

    public boolean decayEnabled() {
        return decayEnabled;
    }

    public int inactiveDaysThreshold() {
        return inactiveDaysThreshold;
    }

    public double decaySessionsPerDay() {
        return decaySessionsPerDay;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double asDouble(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
