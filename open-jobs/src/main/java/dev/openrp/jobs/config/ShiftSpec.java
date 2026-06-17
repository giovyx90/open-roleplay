package dev.openrp.jobs.config;

import java.time.LocalTime;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Optional work shifts: time windows where a job pays more. The night shift is the richest but, in RP,
 * the most dangerous - fewer agents on duty, fewer witnesses. The plugin only applies the multiplier;
 * the danger is the server's narrative, not a mechanic. Windows are matched against the real wall clock
 * and may wrap past midnight (e.g. 22:00-06:00).
 */
public final class ShiftSpec {

    private final boolean enabled;
    private final LocalTime peakStart;
    private final LocalTime peakEnd;
    private final double peakMultiplier;
    private final LocalTime nightStart;
    private final LocalTime nightEnd;
    private final double nightMultiplier;

    private ShiftSpec(boolean enabled, LocalTime peakStart, LocalTime peakEnd, double peakMultiplier,
                      LocalTime nightStart, LocalTime nightEnd, double nightMultiplier) {
        this.enabled = enabled;
        this.peakStart = peakStart;
        this.peakEnd = peakEnd;
        this.peakMultiplier = peakMultiplier <= 0 ? 1.0 : peakMultiplier;
        this.nightStart = nightStart;
        this.nightEnd = nightEnd;
        this.nightMultiplier = nightMultiplier <= 0 ? 1.0 : nightMultiplier;
    }

    public static ShiftSpec disabled() {
        return new ShiftSpec(false, null, null, 1.0, null, null, 1.0);
    }

    public static ShiftSpec from(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return disabled();
        }
        ConfigurationSection peak = section.getConfigurationSection("peak_hours");
        ConfigurationSection night = section.getConfigurationSection("night_shift");
        return new ShiftSpec(true,
                time(peak, "start"), time(peak, "end"), peak == null ? 1.0 : peak.getDouble("multiplier", 1.0),
                time(night, "start"), time(night, "end"), night == null ? 1.0 : night.getDouble("multiplier", 1.0));
    }

    private static LocalTime time(ConfigurationSection section, String key) {
        if (section == null) {
            return null;
        }
        String raw = section.getString(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String[] parts = raw.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalTime.of(Math.floorMod(hour, 24), Math.floorMod(minute, 60));
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public boolean enabled() {
        return enabled;
    }

    /** Pay multiplier in effect at wall-clock {@code now}; night takes precedence over peak. */
    public double multiplierAt(LocalTime now) {
        if (!enabled || now == null) {
            return 1.0;
        }
        if (within(now, nightStart, nightEnd)) {
            return nightMultiplier;
        }
        if (within(now, peakStart, peakEnd)) {
            return peakMultiplier;
        }
        return 1.0;
    }

    /** Inclusive-start, exclusive-end window membership, handling windows that wrap past midnight. */
    private static boolean within(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }
}
