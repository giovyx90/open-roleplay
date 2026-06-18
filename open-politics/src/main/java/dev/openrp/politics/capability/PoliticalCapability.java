package dev.openrp.politics.capability;

import java.util.Locale;
import java.util.Optional;

/**
 * An abstract authority to perform a class of political actions. Like Open FDO and Open Crime, the
 * core defines <em>what</em> each capability unlocks; the configured charges (in {@code charges.yml})
 * decide <em>which charge holds it</em>. The core never hardcodes a charge name.
 *
 * <p>A capability never produces an automatic effect: it enables a command and certifies that whoever
 * acted held the authority to act. {@link #ALL} is a wildcard typically reserved for staff override;
 * a holder of it passes every capability check.</p>
 */
public enum PoliticalCapability {

    /** Sign an official act. */
    SIGN_ACT,
    /** Promulgate a law (subject to the configured legislative iter). */
    SIGN_LAW,
    /** Appoint the holder of another charge. */
    APPOINT,
    /** Remove the holder of another charge. */
    REMOVE,
    /** Dissolve a collegiate body. */
    DISSOLVE,
    /** Declare a state of emergency (Open FDO adapter). */
    DECLARE_EMERGENCY,
    /** Access the public budget (Open Economy adapter). */
    MANAGE_BUDGET,
    /** Revoke company licences (Open Companies adapter). */
    REVOKE_LICENSE,
    /** Call a new election. */
    CALL_ELECTION,
    /** Veto an act within its veto window. */
    VETO,
    /** Wildcard: holding this passes every capability check. */
    ALL;

    /** Case-insensitive lookup; empty for unknown ids so a config typo is skipped, not fatal. */
    public static Optional<PoliticalCapability> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(PoliticalCapability.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
