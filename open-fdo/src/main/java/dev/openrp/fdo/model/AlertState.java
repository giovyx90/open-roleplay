package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * The server-wide alert state. A single value, declared and cleared by members holding the
 * {@code DECLARE_ALERT} capability. {@code level} is abstract; what it means is up to the setting.
 */
public record AlertState(int level, String reason, UUID declaredBy, long declaredAt) {

    public boolean active() {
        return level > 0;
    }

    /** The cleared state. */
    public static AlertState none() {
        return new AlertState(0, "", null, 0L);
    }
}
