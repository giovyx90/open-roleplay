package dev.openrp.vending.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import dev.openrp.vending.adapter.StorageAdapter;
import dev.openrp.vending.model.VendingMachine;

/**
 * Non-persistent storage adapter that keeps machines in memory only. Handy for the
 * {@code adapters.storage: memory} demo mode and for unit-testing the manager without touching
 * disk. Data is lost on restart.
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<UUID, VendingMachine> machines = new LinkedHashMap<>();

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // nothing to open
    }

    @Override
    public Collection<VendingMachine> loadAll() {
        return new ArrayList<>(machines.values());
    }

    @Override
    public void save(VendingMachine machine) {
        machines.put(machine.id(), machine);
    }

    @Override
    public void saveAll(Collection<VendingMachine> all) {
        machines.clear();
        for (VendingMachine machine : all) {
            machines.put(machine.id(), machine);
        }
    }

    @Override
    public void delete(UUID machineId) {
        machines.remove(machineId);
    }

    @Override
    public void flush() {
        // in memory, always durable for the lifetime of the process
    }

    @Override
    public void close() {
        // nothing to release
    }
}
