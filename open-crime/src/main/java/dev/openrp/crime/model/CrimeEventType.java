package dev.openrp.crime.model;

import java.util.Locale;

/**
 * The kind of illegal action a {@link CrimeEvent} records. Setting-neutral: the core knows a crime
 * <em>happened</em>, never what substance or method it involved.
 */
public enum CrimeEventType {

    PRODUCTION,
    TRAFFIC,
    EXTORTION,
    LAUNDERING,
    TERRITORY;

    public static CrimeEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return PRODUCTION;
        }
        try {
            return CrimeEventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return PRODUCTION;
        }
    }
}
