package dev.openrp.companies.config;

import java.util.Locale;

/**
 * How companies come into existence on this server. Selected in {@code config.yml} under
 * {@code companies.creation.mode}; the same code paths support all three without changes.
 */
public enum CreationMode {
    /** Players may found companies freely with {@code /company create} (subject to limits/cost). */
    PLAYER_DIRECT,
    /** Players submit {@code /company apply}; staff approve or deny before the company exists. */
    PLAYER_APPLICATION,
    /** Only staff may create companies, via {@code /company admin create <owner> ...}. */
    ADMIN_ONLY;

    /** Whether a normal player may directly create a company in this mode. */
    public boolean allowsDirectPlayerCreate() {
        return this == PLAYER_DIRECT;
    }

    /** Whether a normal player may submit an application in this mode. */
    public boolean allowsApplication() {
        return this == PLAYER_APPLICATION;
    }

    /** Lenient parser; unknown values fall back to {@link #PLAYER_DIRECT}. */
    public static CreationMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return PLAYER_DIRECT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PLAYER_DIRECT;
        }
    }
}
