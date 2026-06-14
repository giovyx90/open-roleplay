package dev.openrp.access.model;

import java.util.Locale;

public enum AccessPreset {
    PRIVATE("Privato"),
    MEMBERS("Membri"),
    MANAGERS("Manager"),
    PUBLIC("Pubblico"),
    CUSTOM("Custom");

    private final String displayName;

    AccessPreset(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static AccessPreset parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PRIVATE;
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
