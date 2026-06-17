package dev.openrp.jobs.adapter;

import java.util.UUID;

/**
 * Pays out wages. The primary integration point: a session payout is delivered through {@link #pay}
 * as a labelled transaction (e.g. {@code JOB_PAYOUT}). The bundled default reports {@link #available()}
 * as {@code false}, so without an economy plugin the payout falls back to physical items per
 * {@code fallback.no_economy_payout}. An Open Economy bridge registers a real implementation.
 */
public interface EconomyAdapter {

    String id();

    /** Whether a real economy backend is present. When false, payment falls back to physical items. */
    boolean available();

    /** Credits {@code amount} to the player. Returns {@code false} (changing nothing) on failure. */
    boolean pay(UUID player, double amount, String reason);

    boolean has(UUID player, double amount);
}
