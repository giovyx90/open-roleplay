package dev.openrp.vending.config;

import java.util.Locale;

/** How a restock action sources the items it loads into a machine. */
public enum RestockMode {
    /** Consume real items from the staff member's own inventory. */
    PLAYER_INVENTORY,
    /** Pull items from the owning company's warehouse via the adapters. */
    BUSINESS_WAREHOUSE,
    /** No item cost - the management UI simply sets the stock number (admin/demo friendly). */
    FREE;

    public static RestockMode fromString(String value) {
        if (value == null) {
            return PLAYER_INVENTORY;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PLAYER_INVENTORY;
        }
    }
}
