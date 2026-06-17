package dev.openrp.jobs.model;

import java.util.Locale;

/** Lifecycle of a {@link WorkSession}. A session is paused when the worker leaves the region and abandoned if they stay out too long. */
public enum SessionStatus {

    ACTIVE,
    PAUSED,
    COMPLETED,
    ABANDONED;

    public static SessionStatus fromString(String raw) {
        if (raw == null) {
            return ACTIVE;
        }
        try {
            return SessionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return ACTIVE;
        }
    }
}
