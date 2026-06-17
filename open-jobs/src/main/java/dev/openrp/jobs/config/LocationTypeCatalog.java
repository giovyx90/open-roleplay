package dev.openrp.jobs.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Catalogue of {@link LocationType}s from {@code location_types.yml}, rebuilt on each reload. */
public final class LocationTypeCatalog {

    private final Map<String, LocationType> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection root) {
        byId.clear();
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            LocationType type = LocationType.from(key, root.getConfigurationSection(key));
            if (type != null) {
                byId.put(key, type);
            }
        }
    }

    public Optional<LocationType> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<LocationType> all() {
        return byId.values();
    }
}
