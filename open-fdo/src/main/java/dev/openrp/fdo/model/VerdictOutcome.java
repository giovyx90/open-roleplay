package dev.openrp.fdo.model;

import java.util.Locale;

/** The outcome recorded in Section C of a dossier. */
public enum VerdictOutcome {

    /** Convicted; a sentence (and optional detention) follows. */
    GUILTY,
    /** Acquitted. */
    ACQUITTED,
    /** Proceeding dismissed. */
    DISMISSED;

    public boolean carriesSentence() {
        return this == GUILTY;
    }

    public String messageKey() {
        return "verdict.outcome." + name().toLowerCase(Locale.ROOT);
    }

    public static VerdictOutcome fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
