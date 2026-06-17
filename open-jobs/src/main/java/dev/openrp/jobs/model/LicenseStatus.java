package dev.openrp.jobs.model;

import java.util.Locale;

/** State of a {@link WorkLicense}. A revoked or expired licence blocks new sessions for that job. */
public enum LicenseStatus {

    ACTIVE,
    REVOKED,
    EXPIRED;

    public static LicenseStatus fromString(String raw) {
        if (raw == null) {
            return ACTIVE;
        }
        try {
            return LicenseStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return ACTIVE;
        }
    }
}
