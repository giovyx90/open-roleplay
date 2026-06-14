package dev.openrp.access.model;

import java.util.Locale;

public enum AccessPrincipalType {
    PUBLIC,
    PLAYER,
    PROPERTY_OWNER,
    PROPERTY_MEMBER,
    COMPANY_MEMBER,
    COMPANY_MANAGER,
    COMPANY_DIRECTOR,
    COMPANY_OWNER,
    HOTEL_GUEST,
    OVERRIDE_MARKER;

    public static AccessPrincipalType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Il tipo principal e' obbligatorio.");
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
