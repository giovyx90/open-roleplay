package dev.openrp.companies.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Hierarchy of company roles, ordered by {@link #level()} (higher = more authority). Each role is
 * pre-mapped to the {@link CompanyCapability capabilities} it grants; the mapping is cumulative in
 * spirit but defined explicitly per role so it stays readable and easy to override by swapping this
 * enum's wiring. {@link #CEO} always holds {@link CompanyCapability#ADMIN} and therefore grants every
 * capability.
 */
public enum CompanyRole {
    TRAINING(1, EnumSet.of(CompanyCapability.VIEW, CompanyCapability.USE_ASSETS)),
    EMPLOYEE(2, EnumSet.of(CompanyCapability.VIEW, CompanyCapability.USE_ASSETS)),
    MANAGER(3, EnumSet.of(CompanyCapability.VIEW, CompanyCapability.USE_ASSETS,
            CompanyCapability.INVITE, CompanyCapability.MANAGE_ASSETS)),
    VICE_DIRECTOR(4, EnumSet.of(CompanyCapability.VIEW, CompanyCapability.USE_ASSETS,
            CompanyCapability.INVITE, CompanyCapability.MANAGE_ASSETS,
            CompanyCapability.FIRE, CompanyCapability.CHANGE_ROLE)),
    DIRECTOR(5, EnumSet.of(CompanyCapability.VIEW, CompanyCapability.USE_ASSETS,
            CompanyCapability.INVITE, CompanyCapability.MANAGE_ASSETS,
            CompanyCapability.FIRE, CompanyCapability.CHANGE_ROLE,
            CompanyCapability.MANAGE_IDENTITY, CompanyCapability.MANAGE_LICENSES,
            CompanyCapability.MANAGE_FINANCE)),
    CEO(6, EnumSet.allOf(CompanyCapability.class));

    private final int level;
    private final Set<CompanyCapability> capabilities;

    CompanyRole(int level, Set<CompanyCapability> capabilities) {
        this.level = level;
        this.capabilities = Collections.unmodifiableSet(capabilities);
    }

    /** Authority level; higher outranks lower. */
    public int level() {
        return level;
    }

    /** Immutable set of capabilities this role grants. */
    public Set<CompanyCapability> capabilities() {
        return capabilities;
    }

    /** Whether this role grants the given capability (ADMIN implies everything). */
    public boolean grants(CompanyCapability capability) {
        return capability != null
                && (capabilities.contains(CompanyCapability.ADMIN) || capabilities.contains(capability));
    }

    /** Whether this role's authority is greater than or equal to {@code other}. */
    public boolean atLeast(CompanyRole other) {
        return other != null && this.level >= other.level;
    }

    /** Whether this role strictly outranks {@code other}. */
    public boolean outranks(CompanyRole other) {
        return other != null && this.level > other.level;
    }

    /** Translation key (see messages_*.yml) for the human-readable role label. */
    public String messageKey() {
        return "role." + name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; returns {@code null} for unknown values. */
    public static CompanyRole fromString(String value) {
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
