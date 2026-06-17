package dev.openrp.crime.core;

import java.util.Locale;
import java.util.UUID;

/**
 * Id helpers. Organisation ids are slugified from the chosen name (uniqueness enforced by the
 * manager); every other entity uses a short random token so two simultaneous actions never collide.
 */
public final class Ids {

    private Ids() {
    }

    /** A short, URL/key-safe random token (e.g. {@code "a1b2c3d4"}). */
    public static String shortId() {
        return Long.toHexString(UUID.randomUUID().getMostSignificantBits() >>> 1);
    }

    /** Prefixed short id, e.g. {@code prod-a1b2c3d4}. */
    public static String prefixed(String prefix) {
        return prefix + "-" + shortId();
    }

    /**
     * Slugifies an org display name into a stable id seed: lowercase, non-alphanumerics collapsed to
     * a single {@code '_'}. Empty input falls back to a random token. The manager appends a numeric
     * suffix on collision, so the returned value is only a seed.
     */
    public static String slug(String name) {
        if (name == null) {
            return shortId();
        }
        String slug = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? shortId() : slug;
    }
}
