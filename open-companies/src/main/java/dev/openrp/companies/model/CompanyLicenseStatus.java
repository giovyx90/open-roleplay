package dev.openrp.companies.model;

import java.util.Locale;

/**
 * State of a single license held (or sought) by a company.
 *
 * <ul>
 *   <li>{@link #NONE} - the company does not hold the license (default; usually not stored).</li>
 *   <li>{@link #PENDING} - requested, awaiting chamber approval.</li>
 *   <li>{@link #GRANTED} - active and usable.</li>
 *   <li>{@link #SUSPENDED} - temporarily withdrawn, can be reinstated.</li>
 *   <li>{@link #REVOKED} - permanently withdrawn.</li>
 * </ul>
 */
public enum CompanyLicenseStatus {
    NONE,
    PENDING,
    GRANTED,
    SUSPENDED,
    REVOKED;

    /** Whether the license is currently usable. */
    public boolean isActive() {
        return this == GRANTED;
    }

    public String messageKey() {
        return "license_status." + name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; unknown values fall back to {@link #NONE}. */
    public static CompanyLicenseStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
