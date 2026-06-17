package dev.openrp.crime.model;

import java.util.Locale;

/** State of a {@link LaunderingProcess}. */
public enum LaunderingStatus {

    ACTIVE,
    COMPLETED,
    AUDITED;

    public static LaunderingStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return LaunderingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return ACTIVE;
        }
    }
}
