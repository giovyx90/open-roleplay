package dev.openrp.crime.model;

import java.util.Locale;

/** State of a {@link Protection} (extortion / protection contract). */
public enum ProtectionStatus {

    /** Imposed and awaiting the owner's response. */
    PENDING,
    /** Accepted by the owner; periodic payment runs. */
    ACTIVE,
    /** Payment is overdue; an agent must collect physically. */
    OVERDUE,
    /** The owner refused; the org decides how to proceed in RP. */
    REFUSED,
    /** Revoked by the org boss. */
    REVOKED;

    public static ProtectionStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return ProtectionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return PENDING;
        }
    }
}
