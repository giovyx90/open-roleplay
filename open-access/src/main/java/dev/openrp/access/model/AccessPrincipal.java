package dev.openrp.access.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record AccessPrincipal(AccessPrincipalType type, String value) {

    public AccessPrincipal {
        Objects.requireNonNull(type, "type");
        value = normalizeValue(type, value);
    }

    public static AccessPrincipal publicAccess() {
        return new AccessPrincipal(AccessPrincipalType.PUBLIC, "*");
    }

    public static AccessPrincipal player(UUID uuid) {
        return new AccessPrincipal(AccessPrincipalType.PLAYER, uuid == null ? null : uuid.toString());
    }

    public static AccessPrincipal marker() {
        return new AccessPrincipal(AccessPrincipalType.OVERRIDE_MARKER, "*");
    }

    public boolean matches(AccessPrincipal actual) {
        if (actual == null) {
            return false;
        }
        if (type == AccessPrincipalType.PUBLIC) {
            return true;
        }
        return type == actual.type && Objects.equals(value, actual.value);
    }

    private static String normalizeValue(AccessPrincipalType type, String raw) {
        if (type == AccessPrincipalType.PUBLIC || type == AccessPrincipalType.OVERRIDE_MARKER) {
            return "*";
        }
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return type == AccessPrincipalType.PLAYER
                ? raw.trim().toLowerCase(Locale.ROOT)
                : raw.trim().toUpperCase(Locale.ROOT);
    }
}
