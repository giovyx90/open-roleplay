package dev.openrp.jobs.core;

import java.util.UUID;

/** Id helpers: every session, licence and location uses a short random token so simultaneous actions never collide. */
public final class Ids {

    private Ids() {
    }

    /** A short, URL/key-safe random token (e.g. {@code "a1b2c3d4"}). */
    public static String shortId() {
        return Long.toHexString(UUID.randomUUID().getMostSignificantBits() >>> 1);
    }

    /** Prefixed short id, e.g. {@code ses-a1b2c3d4}. */
    public static String prefixed(String prefix) {
        return prefix + "-" + shortId();
    }
}
