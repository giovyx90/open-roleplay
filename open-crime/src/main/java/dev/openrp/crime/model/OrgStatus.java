package dev.openrp.crime.model;

import java.util.Locale;

/** Lifecycle of an {@link IllegalOrg}. The core never moves an org here on its own except dissolution. */
public enum OrgStatus {

    /** Operating normally. */
    ACTIVE,
    /** Wound down by its boss; kept for history but no longer operable. */
    DISSOLVED,
    /** Flagged by the authorities through a discovery-driven investigation. Narrative only. */
    INVESTIGATED;

    public static OrgStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return OrgStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return ACTIVE;
        }
    }
}
