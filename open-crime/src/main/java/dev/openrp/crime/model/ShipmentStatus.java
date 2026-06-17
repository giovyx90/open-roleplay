package dev.openrp.crime.model;

import java.util.Locale;

/** State of a {@link Shipment}. */
public enum ShipmentStatus {

    IN_TRANSIT,
    DELIVERED,
    SEIZED,
    LOST;

    public boolean isTerminal() {
        return this != IN_TRANSIT;
    }

    public static ShipmentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return IN_TRANSIT;
        }
        try {
            return ShipmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return IN_TRANSIT;
        }
    }
}
