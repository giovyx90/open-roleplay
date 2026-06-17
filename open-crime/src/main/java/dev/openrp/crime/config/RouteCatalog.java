package dev.openrp.crime.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Traffic routes, loaded from {@code traffic.yml}. */
public final class RouteCatalog {

    private final Map<String, Route> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection route = section.getConfigurationSection(id);
            if (route == null) {
                continue;
            }
            byId.put(id, new Route(
                    id,
                    route.getString("name", id),
                    route.getString("origin_region", ""),
                    route.getString("destination_region", ""),
                    route.getStringList("waypoints"),
                    route.getDouble("distance_km", 0.0),
                    route.getInt("duration_minutes", 10)));
        }
    }

    public Optional<Route> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Collection<Route> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
