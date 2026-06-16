package dev.openrp.companies.model;

import java.util.Locale;

/**
 * A discrete action a company role may be allowed to perform. The core never hardcodes "what a
 * manager can do"; instead each {@link CompanyRole} maps to a set of these capabilities, and every
 * sensitive operation asks {@code role.grants(capability)}. Servers that want a different power
 * structure only have to change the role&rarr;capability mapping in one place.
 */
public enum CompanyCapability {
    /** See the company, its members and basic info. */
    VIEW,
    /** Invite new members. */
    INVITE,
    /** Remove (fire) members. */
    FIRE,
    /** Promote or demote members. */
    CHANGE_ROLE,
    /** Edit the company display name / prefix / suffix identity. */
    MANAGE_IDENTITY,
    /** Request, accept or surrender licenses. */
    MANAGE_LICENSES,
    /** Register, move or remove company assets. */
    MANAGE_ASSETS,
    /** Operate existing company assets (terminals, POS, printers, ...). */
    USE_ASSETS,
    /** Move company funds. */
    MANAGE_FINANCE,
    /** Full control of the company (implies every other capability). */
    ADMIN;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; returns {@code null} for unknown values. */
    public static CompanyCapability fromString(String value) {
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
