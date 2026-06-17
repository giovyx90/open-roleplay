package dev.openrp.fdo.model;

import java.util.Locale;

/** A step in an evidence item's chain of custody. */
public enum CustodyAction {

    /** Collected at the scene. */
    COLLECTED,
    /** Handed from one agent to another. */
    TRANSFERRED,
    /** Deposited into an evidence locker. */
    DEPOSITED,
    /** Withdrawn from the locker. */
    WITHDRAWN,
    /** Released / returned. */
    RELEASED;

    public String messageKey() {
        return "evidence.action." + name().toLowerCase(Locale.ROOT);
    }

    public static CustodyAction fromString(String value) {
        if (value == null) {
            return COLLECTED;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return COLLECTED;
        }
    }
}
