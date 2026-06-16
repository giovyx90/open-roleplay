package dev.openrp.companies.model;

import java.util.Locale;

/**
 * Legal/operational status of a company, controlled by the chamber of commerce flow.
 *
 * <p>Only {@link #ACTIVE} companies may operate (use assets, trade, hire). {@link #SUSPENDED} is a
 * temporary administrative freeze; {@link #DISSOLVED} marks a company that has been shut down.</p>
 */
public enum CompanyStatus {
    ACTIVE,
    SUSPENDED,
    DISSOLVED;

    /** Whether the company is allowed to operate (trade, use assets, hire) in this status. */
    public boolean canOperate() {
        return this == ACTIVE;
    }

    /** Translation key (see messages_*.yml) for the human-readable status label. */
    public String messageKey() {
        return "status." + name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; unknown values fall back to {@link #ACTIVE}. */
    public static CompanyStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
