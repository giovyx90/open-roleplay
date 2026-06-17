package dev.openrp.fdo.model;

import java.util.Locale;

/** Lifecycle of a dossier. Labels are resolved by the message layer from {@link #messageKey()}. */
public enum DossierStatus {

    /** Section A signed, proceeding open. */
    OPEN,
    /** Investigative phase (Section B in progress). */
    INVESTIGATION,
    /** Awaiting or undergoing trial. */
    TRIAL,
    /** Section C signed, proceeding closed. */
    CLOSED,
    /** Filed away; no further changes expected. */
    ARCHIVED;

    public String messageKey() {
        return "dossier.status." + name().toLowerCase(Locale.ROOT);
    }

    public static DossierStatus fromString(String value) {
        if (value == null) {
            return OPEN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return OPEN;
        }
    }
}
