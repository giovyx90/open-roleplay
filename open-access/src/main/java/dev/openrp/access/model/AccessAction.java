package dev.openrp.access.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum AccessAction {
    OPEN,
    CONTAINER,
    SIGNAL,
    MACHINE,
    PLACE,
    BREAK,
    MANAGE;

    public static final Set<AccessAction> USE_ACTIONS = EnumSet.of(OPEN, CONTAINER, SIGNAL, MACHINE);
    public static final Set<AccessAction> ALL_ACTIONS = EnumSet.allOf(AccessAction.class);

    public static AccessAction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("L'azione accesso e' obbligatoria.");
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
