package dev.openrp.jobs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.model.WorkLocation;

/**
 * Manages the work locations: the physical places, bound to region ids, where jobs happen. A location
 * is where work accrues; capacity (when set) caps simultaneous sessions there. Locations are persisted
 * through the storage adapter and resolved against the region adapter, so the core never assumes
 * WorldGuard.
 */
public final class LocationManager {

    private final OpenJobsPlugin plugin;
    private final java.util.Map<String, WorkLocation> byId = new ConcurrentHashMap<>();

    public LocationManager(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byId.clear();
        for (WorkLocation location : plugin.adapters().storage().loadLocations()) {
            byId.put(location.id(), location);
        }
    }

    public Optional<WorkLocation> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public List<WorkLocation> all() {
        return new ArrayList<>(byId.values());
    }

    public List<WorkLocation> forJob(String jobId) {
        List<WorkLocation> result = new ArrayList<>();
        for (WorkLocation location : byId.values()) {
            if (location.active() && location.jobId().equals(jobId)) {
                result.add(location);
            }
        }
        return result;
    }

    /** The active work location whose region id matches {@code regionId}, if any. */
    public Optional<WorkLocation> byRegion(String regionId) {
        if (regionId == null) {
            return Optional.empty();
        }
        for (WorkLocation location : byId.values()) {
            if (location.active() && regionId.equals(location.regionId())) {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }

    /** Resolves the work location the given world position sits in, via the region adapter. */
    public Optional<WorkLocation> at(Location where) {
        return plugin.adapters().region().regionAt(where).flatMap(this::byRegion);
    }

    public WorkLocation add(String jobId, String regionId, String displayName, int capacity, boolean seasonal) {
        WorkLocation location = new WorkLocation(Ids.prefixed("loc"), jobId, displayName, regionId, capacity, seasonal);
        byId.put(location.id(), location);
        plugin.adapters().storage().saveLocation(location);
        return location;
    }

    public boolean remove(String id) {
        WorkLocation removed = byId.remove(id);
        if (removed != null) {
            plugin.adapters().storage().deleteLocation(id);
            return true;
        }
        return false;
    }
}
