package dev.openrp.jobs.adapter.defaults;

import java.util.UUID;
import dev.openrp.jobs.adapter.EconomyAdapter;

/**
 * Default economy adapter when no economy plugin is present: reports {@link #available()} as
 * {@code false} and never pays. The payment service then falls back to physical items per
 * {@code fallback.no_economy_payout}, so wages are still delivered without any economy backend.
 */
public final class NoopEconomyAdapter implements EconomyAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean pay(UUID player, double amount, String reason) {
        return false;
    }

    @Override
    public boolean has(UUID player, double amount) {
        return false;
    }
}
