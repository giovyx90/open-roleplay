package dev.openrp.access.model;

import java.util.Locale;

public enum AccessProfileType {
    PROPERTY,
    COMPANY,
    HOTEL_ROOM,
    REGION;

    public static AccessProfileType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Il tipo profilo e' obbligatorio.");
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
