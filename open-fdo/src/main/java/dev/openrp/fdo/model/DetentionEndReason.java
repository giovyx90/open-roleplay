package dev.openrp.fdo.model;

import java.util.Locale;

/** Why a detention ended. Passed to the detention adapter and recorded in the dossier. */
public enum DetentionEndReason {

    /** The sentence timer elapsed; the core released automatically. */
    SENTENCE_SERVED,
    /** Released early by an authorised member. */
    RELEASED,
    /** Escape confirmed. */
    ESCAPE,
    /** Transferred to another facility. */
    TRANSFER;

    public String messageKey() {
        return "detention.reason." + name().toLowerCase(Locale.ROOT);
    }
}
