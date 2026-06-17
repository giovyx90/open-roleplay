package dev.openrp.crime.adapter;

import java.util.UUID;

/**
 * Holds organisation money, split into <em>clean</em> and <em>dirty</em> balances per treasury id.
 * Proceeds of crime are deposited dirty; laundering converts dirty into clean. The bundled internal
 * ledger persists balances itself; an Open Bank bridge can register a real implementation that backs
 * the same treasury ids with actual bank accounts and audit hooks.
 */
public interface EconomyAdapter {

    String id();

    /** Whether the backend distinguishes dirty money (needed for laundering to be meaningful). */
    boolean supportsDirtyMoney();

    long balance(UUID treasury, boolean dirty);

    void deposit(UUID treasury, long amount, boolean dirty);

    /** Removes {@code amount} of the given kind; returns {@code false} (changing nothing) if short. */
    boolean withdraw(UUID treasury, long amount, boolean dirty);
}
