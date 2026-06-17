package dev.openrp.jobs.adapter.defaults;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.jobs.adapter.StorageAdapter;
import dev.openrp.jobs.model.LicenseStatus;
import dev.openrp.jobs.model.SessionStatus;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkLocation;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/**
 * Default storage adapter persisting every record to a single YAML file. The schema mirrors the model
 * so the file doubles as readable documentation. Each mutation rewrites the file durably: it is written
 * to a temp sibling and atomically renamed over the live file, with the previous contents kept as
 * {@code .bak}, so a crash mid-write can never corrupt the whole data set (mirrors the Open Crime, Open
 * FDO and Open Companies adapters).
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String LOCATIONS = "locations";
    private static final String LICENSES = "licenses";
    private static final String SESSIONS = "sessions";
    private static final String RECORDS = "records";

    private final File file;
    private final File tempFile;
    private final File backupFile;
    private final Logger logger;
    private YamlConfiguration yaml = new YamlConfiguration();

    public YamlStorageAdapter(File file, Logger logger) {
        this.file = file;
        this.tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        this.backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        this.logger = logger;
    }

    @Override
    public String id() {
        return "yaml";
    }

    @Override
    public void init() {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        YamlConfiguration loaded = tryLoad(file);
        if (loaded == null) {
            loaded = tryLoad(tempFile);
            if (loaded != null) {
                logger.warning("[OpenJobs] Recovered data from interrupted write (" + tempFile.getName() + ").");
            }
        }
        if (loaded == null) {
            loaded = tryLoad(backupFile);
            if (loaded != null) {
                logger.warning("[OpenJobs] Primary data unreadable; recovered from backup (" + backupFile.getName() + ").");
            }
        }
        yaml = loaded != null ? loaded : new YamlConfiguration();
    }

    private YamlConfiguration tryLoad(File source) {
        if (source == null || !source.isFile()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(source);
            return config;
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            logger.severe("[OpenJobs] Failed to read '" + source.getName() + "': " + exception.getMessage());
            return null;
        }
    }

    // --- locations ---------------------------------------------------------------------------

    @Override
    public Collection<WorkLocation> loadLocations() {
        List<WorkLocation> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(LOCATIONS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                WorkLocation location = new WorkLocation(
                        section.getString("id", key),
                        section.getString("job", ""),
                        section.getString("name", key),
                        section.getString("region", ""),
                        section.getInt("capacity", 0),
                        section.getBoolean("seasonal", false));
                location.setActive(section.getBoolean("active", true));
                result.add(location);
            } catch (RuntimeException exception) {
                logger.warning("[OpenJobs] Skipping malformed location '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveLocation(WorkLocation location) {
        String base = LOCATIONS + "." + escapeKey(location.id());
        yaml.set(base, null);
        yaml.set(base + ".id", location.id());
        yaml.set(base + ".job", location.jobId());
        yaml.set(base + ".name", location.displayName());
        yaml.set(base + ".region", location.regionId());
        yaml.set(base + ".capacity", location.capacity());
        yaml.set(base + ".seasonal", location.seasonal());
        yaml.set(base + ".active", location.active());
        persist();
    }

    @Override
    public void deleteLocation(String locationId) {
        yaml.set(LOCATIONS + "." + escapeKey(locationId), null);
        persist();
    }

    // --- licences ----------------------------------------------------------------------------

    @Override
    public Collection<WorkLicense> loadLicenses() {
        List<WorkLicense> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(LICENSES);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID player = parseUuid(section.getString("player"));
            if (player == null) {
                continue;
            }
            try {
                WorkLicense license = new WorkLicense(
                        section.getString("id", key),
                        player,
                        section.getString("job", ""),
                        section.getLong("issued-at"),
                        section.getString("issued-by", "system"));
                license.setStatus(LicenseStatus.fromString(section.getString("status")));
                license.setItemUuid(emptyToNull(section.getString("item-uuid")));
                result.add(license);
            } catch (RuntimeException exception) {
                logger.warning("[OpenJobs] Skipping malformed licence '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveLicense(WorkLicense license) {
        String base = LICENSES + "." + escapeKey(license.id());
        yaml.set(base, null);
        yaml.set(base + ".id", license.id());
        yaml.set(base + ".player", license.player().toString());
        yaml.set(base + ".job", license.jobId());
        yaml.set(base + ".issued-at", license.issuedAt());
        yaml.set(base + ".issued-by", license.issuedBy());
        yaml.set(base + ".status", license.status().name());
        yaml.set(base + ".item-uuid", license.itemUuid());
        persist();
    }

    @Override
    public void deleteLicense(String licenseId) {
        yaml.set(LICENSES + "." + escapeKey(licenseId), null);
        persist();
    }

    // --- sessions ----------------------------------------------------------------------------

    @Override
    public Collection<WorkSession> loadSessions() {
        List<WorkSession> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(SESSIONS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID player = parseUuid(section.getString("player"));
            if (player == null) {
                continue;
            }
            try {
                WorkSession session = new WorkSession(
                        section.getString("id", key),
                        player,
                        section.getString("job", ""),
                        section.getString("location", ""),
                        section.getLong("started-at"));
                // Stop the clock on the recovered session: banked time is authoritative, the clock
                // restarts only when the worker is back in the region.
                session.bankActiveTime(section.getLong("started-at"));
                session.setActiveMillisBanked(section.getLong("active-millis"));
                session.setStatus(SessionStatus.fromString(section.getString("status")));
                session.setProgressionTier(section.getString("tier", ""));
                session.setLeftRegionAt(section.getLong("left-region-at"));
                ConfigurationSection produced = section.getConfigurationSection("produced");
                if (produced != null) {
                    for (String material : produced.getKeys(false)) {
                        session.putProduced(material, produced.getInt(material));
                    }
                }
                result.add(session);
            } catch (RuntimeException exception) {
                logger.warning("[OpenJobs] Skipping malformed session '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveSession(WorkSession session) {
        String base = SESSIONS + "." + escapeKey(session.id());
        yaml.set(base, null);
        yaml.set(base + ".id", session.id());
        yaml.set(base + ".player", session.player().toString());
        yaml.set(base + ".job", session.jobId());
        yaml.set(base + ".location", session.locationId());
        yaml.set(base + ".started-at", session.startedAt());
        yaml.set(base + ".active-millis", session.activeMillis(System.currentTimeMillis()));
        yaml.set(base + ".status", session.status().name());
        yaml.set(base + ".tier", session.progressionTier());
        yaml.set(base + ".left-region-at", session.leftRegionAt());
        for (var entry : session.producedByMaterial().entrySet()) {
            yaml.set(base + ".produced." + escapeKey(entry.getKey()), entry.getValue());
        }
        persist();
    }

    @Override
    public void deleteSession(String sessionId) {
        yaml.set(SESSIONS + "." + escapeKey(sessionId), null);
        persist();
    }

    // --- records -----------------------------------------------------------------------------

    @Override
    public Collection<WorkRecord> loadRecords() {
        List<WorkRecord> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(RECORDS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID player = parseUuid(section.getString("player"));
            if (player == null) {
                continue;
            }
            try {
                WorkRecord record = new WorkRecord(player, section.getString("job", ""));
                record.setTotalSessions(section.getInt("total-sessions"));
                record.setTotalProduced(section.getLong("total-produced"));
                record.setTotalPayout(section.getDouble("total-payout"));
                record.setCurrentTier(section.getString("tier", ""));
                record.setFirstSessionAt(section.getLong("first-session-at"));
                record.setLastSessionAt(section.getLong("last-session-at"));
                record.setDecayedSessions(section.getDouble("decayed-sessions"));
                result.add(record);
            } catch (RuntimeException exception) {
                logger.warning("[OpenJobs] Skipping malformed record '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveRecord(WorkRecord record) {
        String base = RECORDS + "." + escapeKey(record.player() + "|" + record.jobId());
        yaml.set(base, null);
        yaml.set(base + ".player", record.player().toString());
        yaml.set(base + ".job", record.jobId());
        yaml.set(base + ".total-sessions", record.totalSessions());
        yaml.set(base + ".total-produced", record.totalProduced());
        yaml.set(base + ".total-payout", record.totalPayout());
        yaml.set(base + ".tier", record.currentTier());
        yaml.set(base + ".first-session-at", record.firstSessionAt());
        yaml.set(base + ".last-session-at", record.lastSessionAt());
        yaml.set(base + ".decayed-sessions", record.decayedSessions());
        persist();
    }

    // --- lifecycle ---------------------------------------------------------------------------

    @Override
    public void flush() {
        persist();
    }

    @Override
    public void close() {
        persist();
    }

    // --- helpers -----------------------------------------------------------------------------

    private static String escapeKey(String id) {
        // '.' is the configuration path separator. Percent-encode it (and the escape char itself) so the
        // mapping is reversible and injective: two ids differing only by '.' can never collide on the
        // same YAML key. '%' -> %25, '.' -> %2E.
        if (id == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '%') {
                out.append("%25");
            } else if (c == '.') {
                out.append("%2E");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Durably writes the current state: temp file, then copy the live file aside as {@code .bak}, then
     * atomically rename the temp over the live file. A crash at any point leaves either the previous
     * complete file or a recoverable {@code .tmp}/{@code .bak}.
     */
    private void persist() {
        try {
            yaml.save(tempFile);
        } catch (IOException exception) {
            logger.severe("[OpenJobs] Failed to write data: " + exception.getMessage());
            return;
        }
        if (file.exists()) {
            try {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.warning("[OpenJobs] Failed to refresh data backup: " + exception.getMessage());
            }
        }
        try {
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicUnsupported) {
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.severe("[OpenJobs] Failed to commit data: " + exception.getMessage());
            }
        } catch (IOException exception) {
            logger.severe("[OpenJobs] Failed to commit data: " + exception.getMessage());
        }
    }
}
