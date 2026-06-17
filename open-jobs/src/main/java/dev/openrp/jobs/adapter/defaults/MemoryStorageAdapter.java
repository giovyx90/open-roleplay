package dev.openrp.jobs.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.jobs.adapter.StorageAdapter;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkLocation;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * In-memory storage adapter: useful for tests and ephemeral servers. Holds everything in maps and
 * loses it on shutdown. Records are keyed by {@code player|job} since progression is per (player, job).
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<String, WorkLocation> locations = new ConcurrentHashMap<>();
    private final Map<String, WorkLicense> licenses = new ConcurrentHashMap<>();
    private final Map<String, WorkSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, WorkRecord> records = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // nothing to open
    }

    @Override
    public Collection<WorkLocation> loadLocations() {
        return new ArrayList<>(locations.values());
    }

    @Override
    public void saveLocation(WorkLocation location) {
        locations.put(location.id(), location);
    }

    @Override
    public void deleteLocation(String locationId) {
        locations.remove(locationId);
    }

    @Override
    public Collection<WorkLicense> loadLicenses() {
        return new ArrayList<>(licenses.values());
    }

    @Override
    public void saveLicense(WorkLicense license) {
        licenses.put(license.id(), license);
    }

    @Override
    public void deleteLicense(String licenseId) {
        licenses.remove(licenseId);
    }

    @Override
    public Collection<WorkSession> loadSessions() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public void saveSession(WorkSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public Collection<WorkRecord> loadRecords() {
        return new ArrayList<>(records.values());
    }

    @Override
    public void saveRecord(WorkRecord record) {
        records.put(record.player() + "|" + record.jobId(), record);
    }

    @Override
    public void flush() {
        // nothing to flush
    }

    @Override
    public void close() {
        // nothing to close
    }
}
