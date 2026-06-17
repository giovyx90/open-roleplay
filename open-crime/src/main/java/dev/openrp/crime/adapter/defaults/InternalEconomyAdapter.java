package dev.openrp.crime.adapter.defaults;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.openrp.crime.adapter.EconomyAdapter;
import dev.openrp.crime.adapter.StorageAdapter;

/**
 * Default economy: a small internal ledger persisted through the storage adapter. Each treasury holds
 * a clean and a dirty balance. It is the bundled fallback so laundering and crime proceeds work with
 * no external economy; an Open Bank bridge can register a richer {@link EconomyAdapter} that replaces
 * it. Balances are guarded by the instance monitor - mutations are infrequent and short.
 */
public final class InternalEconomyAdapter implements EconomyAdapter {

    private final StorageAdapter storage;
    private final Map<UUID, long[]> balances = new HashMap<>();

    public InternalEconomyAdapter(StorageAdapter storage) {
        this.storage = storage;
    }

    /** Loads persisted balances. Call after the storage adapter has been initialised. */
    public synchronized void load() {
        balances.clear();
        balances.putAll(storage.loadTreasuries());
    }

    @Override
    public String id() {
        return "internal";
    }

    @Override
    public boolean supportsDirtyMoney() {
        return true;
    }

    @Override
    public synchronized long balance(UUID treasury, boolean dirty) {
        long[] entry = balances.get(treasury);
        if (entry == null) {
            return 0L;
        }
        return dirty ? entry[1] : entry[0];
    }

    @Override
    public synchronized void deposit(UUID treasury, long amount, boolean dirty) {
        if (treasury == null || amount <= 0L) {
            return;
        }
        long[] entry = balances.computeIfAbsent(treasury, key -> new long[]{0L, 0L});
        int index = dirty ? 1 : 0;
        // Saturate instead of wrapping to a negative balance on overflow.
        entry[index] = amount > Long.MAX_VALUE - entry[index] ? Long.MAX_VALUE : entry[index] + amount;
        storage.saveTreasury(treasury, entry[0], entry[1]);
    }

    @Override
    public synchronized boolean withdraw(UUID treasury, long amount, boolean dirty) {
        if (treasury == null || amount <= 0L) {
            return false;
        }
        long[] entry = balances.get(treasury);
        int index = dirty ? 1 : 0;
        if (entry == null || entry[index] < amount) {
            return false;
        }
        entry[index] -= amount;
        storage.saveTreasury(treasury, entry[0], entry[1]);
        return true;
    }
}
