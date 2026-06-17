package dev.openrp.fdo.model;

import java.util.Locale;

/** Where a piece of evidence currently is in its lifecycle. */
public enum EvidenceState {

    /** Carried by an agent (vulnerable - can physically be lost or stolen before deposit). */
    IN_HAND,
    /** Deposited into an evidence locker. */
    STORED,
    /** Released / returned, no longer tracked. */
    RELEASED;

    public String messageKey() {
        return "evidence.state." + name().toLowerCase(Locale.ROOT);
    }

    public static EvidenceState fromString(String value) {
        if (value == null) {
            return IN_HAND;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return IN_HAND;
        }
    }
}
