package dev.openrp.vending.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-machine reentrant locks. Every purchase, restock and cash withdrawal runs inside the relevant
 * machine's lock, so two actors can never interleave on the same machine and duplicate items or
 * money. Locks are reentrant, so a service that already holds a machine's lock can safely call into
 * the manager (which locks again) on the same thread.
 */
public final class MachineLocks {

    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock get(UUID machineId) {
        return locks.computeIfAbsent(machineId, ignored -> new ReentrantLock());
    }

    public void remove(UUID machineId) {
        locks.remove(machineId);
    }
}
