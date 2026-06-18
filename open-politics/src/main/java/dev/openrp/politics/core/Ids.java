package dev.openrp.politics.core;

import java.util.Locale;
import java.util.UUID;

/**
 * Id helpers. Most entities (holders, elections, acts, collegiate votes) use a short random token so
 * two simultaneous actions never collide; act <em>display</em> ids follow the configurable pattern and
 * are assigned by {@link dev.openrp.politics.core.ActService} from a per-year counter.
 */
public final class Ids {

    private Ids() {
    }

    /** A short, URL/key-safe random token (e.g. {@code "a1b2c3d4"}). */
    public static String shortId() {
        return Long.toHexString(UUID.randomUUID().getMostSignificantBits() >>> 1);
    }

    /** Prefixed short id, e.g. {@code act-a1b2c3d4}. */
    public static String prefixed(String prefix) {
        return prefix + "-" + shortId();
    }

    /** Slugifies a display name into a stable id seed: lowercase, non-alphanumerics collapsed to {@code '_'}. */
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
