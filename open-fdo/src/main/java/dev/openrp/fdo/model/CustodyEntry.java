package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * An immutable link in an evidence chain of custody: who handled the item, what they did, when, and
 * where. A gap in the chain is what makes a piece of evidence contestable in roleplay, so each entry
 * is recorded with plain coordinates and never edited after the fact.
 *
 * @param fromAgent the agent relinquishing custody, or {@code null} for the initial collection
 * @param toAgent   the agent taking custody, or {@code null} for a deposit/release
 */
public record CustodyEntry(UUID fromAgent, UUID toAgent, CustodyAction action, long timestamp,
                           String world, double x, double y, double z) {
}
