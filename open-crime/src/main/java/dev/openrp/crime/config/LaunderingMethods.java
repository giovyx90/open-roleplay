package dev.openrp.crime.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Laundering methods, loaded from {@code laundering.yml}. */
public final class LaunderingMethods {

    private final Map<String, LaunderingMethod> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection method = section.getConfigurationSection(id);
            if (method == null) {
                continue;
            }
            byId.put(id, new LaunderingMethod(
                    id,
                    method.getString("display_name", id),
                    method.getString("requires_adapter", ""),
                    method.getInt("loss_percentage", 0),
                    method.getLong("max_per_day", 0L),
                    method.getInt("detection_risk", 0),
                    method.getInt("duration_hours", 1)));
        }
    }

    public Optional<LaunderingMethod> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Collection<LaunderingMethod> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
