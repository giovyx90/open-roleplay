package dev.openrp.companies.model;

import java.util.Locale;

/**
 * Catalogue of licenses the chamber of commerce can grant a company. Licenses are intentionally
 * generic so vertical modules (food, security, transport, banking, ...) can gate their own gameplay
 * on {@code chamber().hasLicense(companyId, type)} without the company core knowing anything about
 * that gameplay.
 */
public enum CompanyLicenseType {
    GENERAL_BUSINESS,
    FOOD_SERVICE,
    PRIVATE_SECURITY,
    TRANSPORT,
    BANKING,
    MEDIA,
    REAL_ESTATE,
    MANUFACTURING,
    PRIVATE_HEALTHCARE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; returns {@code null} for unknown values. */
    public static CompanyLicenseType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
