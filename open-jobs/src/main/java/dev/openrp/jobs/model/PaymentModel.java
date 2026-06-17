package dev.openrp.jobs.model;

import java.util.Locale;

/**
 * How a job pays out. Setting-neutral: the core never assumes what is mined, cut or crafted - it only
 * knows that work either yields measurable production, occupies a session, or is paid on delivery.
 *
 * <ul>
 *   <li>{@link #A_PRODUZIONE} - paid per unit of valid material produced during the session.</li>
 *   <li>{@link #A_SESSIONE} - paid by effective session duration, with a malus below an activity floor.</li>
 *   <li>{@link #A_CONSEGNA} - nothing during extraction; paid when the worker reaches the delivery point.</li>
 * </ul>
 */
public enum PaymentModel {

    A_PRODUZIONE,
    A_SESSIONE,
    A_CONSEGNA;

    public static PaymentModel fromString(String raw) {
        if (raw == null) {
            return A_PRODUZIONE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "a_sessione", "session", "per_session" -> A_SESSIONE;
            case "a_consegna", "delivery", "on_delivery" -> A_CONSEGNA;
            default -> A_PRODUZIONE;
        };
    }
}
