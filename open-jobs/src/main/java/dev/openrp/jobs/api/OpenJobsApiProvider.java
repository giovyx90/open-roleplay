package dev.openrp.jobs.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.adapter.AdapterRegistry;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.core.LocationManager;
import dev.openrp.jobs.core.ProgressionService;
import dev.openrp.jobs.model.WorkLicense;
import dev.openrp.jobs.model.WorkRecord;
import dev.openrp.jobs.model.WorkSession;

/** Thin delegating implementation of {@link OpenJobsApi} backed by the plugin's live services. */
public final class OpenJobsApiProvider implements OpenJobsApi {

    private final OpenJobsPlugin plugin;

    public OpenJobsApiProvider(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }

    @Override
    public Collection<Job> jobs() {
        return plugin.config().jobs().all();
    }

    @Override
    public Optional<Job> getJob(String jobId) {
        return plugin.config().jobs().get(jobId);
    }

    @Override
    public Optional<WorkSession> getActiveSession(UUID player) {
        return plugin.sessions().byPlayer(player);
    }

    @Override
    public List<WorkSession> activeSessions() {
        return plugin.sessions().active();
    }

    @Override
    public Optional<WorkRecord> getRecord(UUID player, String jobId) {
        return Optional.ofNullable(plugin.records().get(player, jobId));
    }

    @Override
    public List<WorkRecord> getRecords(UUID player) {
        return plugin.records().forPlayer(player);
    }

    @Override
    public Optional<String> getTier(UUID player, String jobId) {
        WorkRecord record = plugin.records().get(player, jobId);
        if (record == null) {
            return Optional.empty();
        }
        return plugin.progression().currentTier(record, System.currentTimeMillis())
                .map(dev.openrp.jobs.config.ProgressionTier::id);
    }

    @Override
    public boolean hasLicense(UUID player, String jobId) {
        return plugin.licenses().hasActive(player, jobId);
    }

    @Override
    public Optional<WorkLicense> getLicense(UUID player, String jobId) {
        return plugin.licenses().get(player, jobId);
    }

    @Override
    public List<WorkLicense> getLicenses(UUID player) {
        return plugin.licenses().forPlayer(player);
    }

    @Override
    public LocationManager locations() {
        return plugin.locations();
    }

    @Override
    public ProgressionService progression() {
        return plugin.progression();
    }
}
