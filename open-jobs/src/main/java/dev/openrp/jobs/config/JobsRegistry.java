package dev.openrp.jobs.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** In-memory catalogue of every configured {@link Job}, rebuilt from {@code jobs.yml} on each reload. */
public final class JobsRegistry {

    private final Map<String, Job> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection root) {
        byId.clear();
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            Job job = Job.from(key, section);
            if (job != null) {
                byId.put(key, job);
            }
        }
    }

    public Optional<Job> get(String jobId) {
        return Optional.ofNullable(jobId == null ? null : byId.get(jobId));
    }

    public boolean exists(String jobId) {
        return jobId != null && byId.containsKey(jobId);
    }

    public Collection<Job> all() {
        return byId.values();
    }

    /** All jobs whose {@code location_type} matches the given id (a location may host several jobs). */
    public List<Job> byLocationType(String locationType) {
        List<Job> result = new ArrayList<>();
        for (Job job : byId.values()) {
            if (job.locationType().equalsIgnoreCase(locationType)) {
                result.add(job);
            }
        }
        return result;
    }
}
