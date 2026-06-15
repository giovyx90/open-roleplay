package dev.openrp.vending.adapter;

import java.util.Locale;

/**
 * Capabilities a company role may grant. The {@code BusinessAdapter} maps a player's role to a set
 * of these; the core never assumes a particular role naming scheme.
 */
public enum BusinessCapability {
    /** Buy from a company machine (rarely restricted, included for completeness). */
    USE,
    /** Refill a machine's stock. */
    RESTOCK,
    /** Withdraw the internal cash box. */
    WITHDRAW,
    /** Change product prices (only effective when price editing is enabled in config). */
    EDIT_PRICE,
    /** Administrative management of the company's machines. */
    MANAGE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BusinessCapability fromString(String value) {
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
