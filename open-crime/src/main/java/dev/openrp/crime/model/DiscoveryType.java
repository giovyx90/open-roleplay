package dev.openrp.crime.model;

import java.util.Locale;

/**
 * How a {@link CrimeEvent} became known to the authorities. This enum is the heart of the design:
 * there is no hidden "heat" number - the authorities only ever learn about a crime through one of
 * these five concrete, RP-driven events.
 */
public enum DiscoveryType {

    /** A civilian witness used {@code /denuncia} next to an authority agent. */
    DENUNCIA,
    /** An agent physically entered an active production location or found illegal goods. */
    SCOPERTA_FISICA,
    /** A member was arrested carrying illegal goods; recent org events were linked to the dossier. */
    ARRESTO,
    /** A member turned informant with {@code /informatore} next to a capable agent. */
    INFORMATORE,
    /** An agent manually linked existing discoveries inside an open dossier. */
    INDAGINE;

    public static DiscoveryType fromString(String value) {
        if (value == null || value.isBlank()) {
            return DENUNCIA;
        }
        try {
            return DiscoveryType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return DENUNCIA;
        }
    }
}
