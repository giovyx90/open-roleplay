package dev.openrp.fdo.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.fdo.adapter.AdapterRegistry;

/**
 * Persisted monotonic counters (dossier sequence per year/corps, badge numbers per corps). Backed by
 * the storage adapter so numbers survive restarts and never collide after a delete. {@link #next} is
 * synchronized so two simultaneous acts can never receive the same number.
 */
public final class Counters {

    private final AdapterRegistry adapters;
    private final Map<String, Long> values = new ConcurrentHashMap<>();

    public Counters(AdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public void loadAll() {
        values.clear();
        values.putAll(adapters.storage().loadCounters());
    }

    /** Atomically increments and persists the counter for {@code key}, returning the new value. */
    public synchronized long next(String key) {
        long value = values.getOrDefault(key, 0L) + 1L;
        values.put(key, value);
        adapters.storage().saveCounter(key, value);
        return value;
    }

    public long current(String key) {
        return values.getOrDefault(key, 0L);
    }
}
