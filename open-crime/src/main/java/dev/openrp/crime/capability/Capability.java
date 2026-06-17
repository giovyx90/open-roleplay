package dev.openrp.crime.capability;

import java.util.Locale;
import java.util.Optional;

/**
 * An abstract permission to perform a class of organisation actions. Like Open FDO, the core defines
 * <em>what</em> each capability unlocks; the configured hierarchy (in {@code syndicate.yml}) decides
 * <em>which rank holds it</em>. The core never hardcodes a rank name.
 *
 * <p>{@link #ALL} is a wildcard typically given to the apical rank: a member holding it passes every
 * capability check. Module-specific capabilities (production, traffic, laundering, racket) gate the
 * matching subsystem; if that subsystem is disabled the capability simply never fires.</p>
 */
public enum Capability {

    /** Invite a new member. */
    INVITE,
    /** Expel a member. */
    EXPEL,
    /** Promote a member to the next rank. */
    PROMOTE,
    /** Demote a member. */
    DEMOTE,
    /** Dissolve the whole organisation. */
    DISSOLVE,
    /** View the organisation treasury. */
    VIEW_TREASURY,
    /** Claim or abandon territory. */
    TERRITORY_CLAIM,
    /** Start a production process. */
    PRODUCE,
    /** Cancel an active production process. */
    PRODUCE_CANCEL,
    /** Run a shipment. */
    TRAFFIC,
    /** Propose a transit agreement with another org. */
    TRAFFIC_AGREEMENT,
    /** Read the shipment log. */
    TRAFFIC_LOG,
    /** Start a laundering process. */
    LAUNDER,
    /** Impose protection on a company. */
    RACKET_IMPOSE,
    /** Collect an overdue protection payment. */
    RACKET_COLLECT,
    /** Escalate pressure on a company. */
    RACKET_ESCALATE,
    /** List protections / revoke a contract. */
    RACKET_MANAGE,
    /** Wildcard: holding this passes every capability check. */
    ALL;

    /** Case-insensitive lookup; empty for unknown ids so a config typo is skipped, not fatal. */
    public static Optional<Capability> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Capability.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
