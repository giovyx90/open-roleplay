package dev.openrp.companies.core;

import java.util.Locale;
import dev.openrp.companies.config.CompaniesSettings;

/**
 * Validates company names and types against the configured rules and turns a display name into a
 * stable id slug. Pure (no Bukkit, no state beyond the settings reference), so all of it is directly
 * unit-testable - the name rules and slug algorithm are part of the module's contract.
 */
public final class CompanyValidator {

    private final CompaniesSettings settings;

    public CompanyValidator(CompaniesSettings settings) {
        this.settings = settings;
    }

    /** Validates a display name; returns a successful result when valid, else a failure with a key. */
    public CompanyResult validateName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return CompanyResult.fail("validation.name_required");
        }
        String trimmed = displayName.trim();
        if (trimmed.length() < settings.nameMinLength()) {
            return CompanyResult.fail("validation.name_too_short", "min", settings.nameMinLength());
        }
        if (trimmed.length() > settings.nameMaxLength()) {
            return CompanyResult.fail("validation.name_too_long", "max", settings.nameMaxLength());
        }
        if (!settings.namePattern().matcher(trimmed).matches()) {
            return CompanyResult.fail("validation.name_invalid_chars");
        }
        if (settings.isReservedName(trimmed)) {
            return CompanyResult.fail("validation.name_reserved", "name", trimmed);
        }
        if (slugify(trimmed).isEmpty()) {
            return CompanyResult.fail("validation.name_invalid_chars");
        }
        return CompanyResult.ok("validation.ok");
    }

    /** Validates that a company type is allowed; returns success or a failure with a key. */
    public CompanyResult validateType(String type) {
        if (type == null || type.isBlank()) {
            return CompanyResult.fail("validation.type_required");
        }
        if (!settings.isTypeAllowed(type)) {
            return CompanyResult.fail("validation.type_not_allowed", "type", type.toLowerCase(Locale.ROOT));
        }
        return CompanyResult.ok("validation.ok");
    }

    /**
     * Turns a display name into a stable, url-/config-friendly id slug: lower-cased, spaces and
     * underscores collapsed to single hyphens, any other non-{@code [a-z0-9-]} character dropped, and
     * leading/trailing hyphens trimmed. {@code "Red Spot Foods"} becomes {@code "red-spot-foods"}.
     */
    public static String slugify(String name) {
        if (name == null) {
            return "";
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        StringBuilder slug = new StringBuilder(lower.length());
        boolean lastHyphen = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                slug.append(c);
                lastHyphen = false;
            } else if (c == ' ' || c == '_' || c == '-') {
                if (!lastHyphen && slug.length() > 0) {
                    slug.append('-');
                    lastHyphen = true;
                }
            }
            // any other character is dropped
        }
        int end = slug.length();
        while (end > 0 && slug.charAt(end - 1) == '-') {
            end--;
        }
        return slug.substring(0, end);
    }
}
