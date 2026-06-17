package dev.openrp.jobs.adapter;

import java.util.Collection;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkLocation;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * Persistence backend for every Open Jobs record. CRUD-shaped so a relational backend can map each
 * {@code save}/{@code delete} to a single-row upsert, while the bundled YAML and in-memory adapters
 * rewrite their structures. The core calls the relevant {@code save} after each mutation, so
 * durability is the adapter's decision. Only in-flight sessions are persisted; completed ones live on
 * as aggregate {@link WorkRecord}s.
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    // --- work locations ----------------------------------------------------------------------

    Collection<WorkLocation> loadLocations();

    void saveLocation(WorkLocation location);

    void deleteLocation(String locationId);

    // --- licences ----------------------------------------------------------------------------

    Collection<WorkLicense> loadLicenses();

    void saveLicense(WorkLicense license);

    void deleteLicense(String licenseId);

    // --- in-flight sessions (active / paused) ------------------------------------------------

    Collection<WorkSession> loadSessions();

    void saveSession(WorkSession session);

    void deleteSession(String sessionId);

    // --- progression records -----------------------------------------------------------------

    Collection<WorkRecord> loadRecords();

    void saveRecord(WorkRecord record);

    // --- lifecycle ---------------------------------------------------------------------------

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
