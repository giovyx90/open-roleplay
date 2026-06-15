package dev.openrp.vending.model;

import java.util.Locale;

/**
 * Lifecycle state of a vending machine.
 *
 * <p>Only {@link #ACTIVE} machines may sell. {@link #EMPTY} is reached automatically when every
 * product is out of stock; {@link #BROKEN} models a realistic jam/fault and {@link #DISABLED} is a
 * manual administrative off-switch.</p>
 */
public enum VendingMachineState {
    ACTIVE("state.active"),
    EMPTY("state.empty"),
    BROKEN("state.broken"),
    DISABLED("state.disabled");

    private final String messageKey;

    VendingMachineState(String messageKey) {
        this.messageKey = messageKey;
    }

    /** Translation key (see messages_*.yml) for the human-readable state label. */
    public String messageKey() {
        return messageKey;
    }

    /** Whether the machine is allowed to complete sales in this state. */
    public boolean canSell() {
        return this == ACTIVE;
    }

    /** Lenient parser used by storage adapters; unknown values fall back to {@link #ACTIVE}. */
    public static VendingMachineState fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
