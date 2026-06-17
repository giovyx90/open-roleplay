package dev.openrp.crime.model;

import java.util.Locale;

/** State of a {@link ProductionProcess}. */
public enum ProductionStatus {

    ACTIVE,
    PAUSED,
    COMPLETED,
    SEIZED;

    public boolean isTerminal() {
        return this == COMPLETED || this == SEIZED;
    }

    public static ProductionStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return ProductionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return ACTIVE;
        }
    }
}
