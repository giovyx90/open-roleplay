package dev.openrp.crime.model;

import java.util.Locale;

/** Lifecycle of a tracked illegal good item. */
public enum TrackedGoodStatus {

    FREE,
    IN_TRANSIT,
    SEIZED,
    SOLD;

    public static TrackedGoodStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return FREE;
        }
        try {
            return TrackedGoodStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return FREE;
        }
    }
}
