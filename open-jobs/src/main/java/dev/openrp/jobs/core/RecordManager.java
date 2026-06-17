package dev.openrp.jobs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.model.WorkRecord;

/**
 * Holds the lifetime {@link WorkRecord} for every (player, job) pair - the substrate of progression.
 * A record exists once a player has worked a job at least once; progression is tracked separately per
 * job, so a player can be a master woodcutter and a novice miner at the same time.
 */
public final class RecordManager {

    private final OpenJobsPlugin plugin;
    private final java.util.Map<String, WorkRecord> byKey = new ConcurrentHashMap<>();

    public RecordManager(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    private static String key(UUID player, String jobId) {
        return player + "|" + jobId;
    }

    public void loadAll() {
        byKey.clear();
        for (WorkRecord record : plugin.adapters().storage().loadRecords()) {
            byKey.put(key(record.player(), record.jobId()), record);
        }
    }

    public WorkRecord getOrCreate(UUID player, String jobId) {
        return byKey.computeIfAbsent(key(player, jobId), unused -> new WorkRecord(player, jobId));
    }

    public WorkRecord get(UUID player, String jobId) {
        return byKey.get(key(player, jobId));
    }

    public void save(WorkRecord record) {
        byKey.put(key(record.player(), record.jobId()), record);
        plugin.adapters().storage().saveRecord(record);
    }

    public List<WorkRecord> forPlayer(UUID player) {
        List<WorkRecord> result = new ArrayList<>();
        for (WorkRecord record : byKey.values()) {
            if (record.player().equals(player)) {
                result.add(record);
            }
        }
        return result;
    }

    public List<WorkRecord> all() {
        return new ArrayList<>(byKey.values());
    }
}
