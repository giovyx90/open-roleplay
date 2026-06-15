package dev.openrp.vending.adapter;

import java.util.Collection;
import java.util.UUID;
import dev.openrp.vending.model.VendingMachine;

/**
 * Persistence backend for machines, stock, cash and state.
 *
 * <p>The interface is intentionally CRUD-shaped so a relational backend (SQLite/MySQL) can implement
 * {@link #save(VendingMachine)} / {@link #delete(UUID)} as single-row upserts/deletes, while the
 * bundled YAML/JSON-style adapters can simply rewrite their file. The core calls {@link #save}
 * after every mutating transaction, so durability is the adapter's decision.</p>
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    /** Loads every persisted machine. Called once on enable, after {@link #init()}. */
    Collection<VendingMachine> loadAll();

    /** Inserts or updates a single machine. */
    void save(VendingMachine machine);

    /** Replaces the entire persisted set (used after bulk operations / reloads). */
    void saveAll(Collection<VendingMachine> machines);

    /** Deletes a machine by id. */
    void delete(UUID machineId);

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
