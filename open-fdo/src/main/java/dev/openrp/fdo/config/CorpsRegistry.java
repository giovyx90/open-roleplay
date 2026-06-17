package dev.openrp.fdo.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Holds the configured corps. Reloadable: {@link #load} clears and refills from {@code corps.yml}. */
public final class CorpsRegistry {

    private final Map<String, Corps> byId = new LinkedHashMap<>();

    /** Replaces the registry contents from the {@code corps} section of {@code corps.yml}. */
    public void load(ConfigurationSection root) {
        byId.clear();
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            byId.put(id, new Corps(
                    id,
                    section.getString("display_name", id),
                    section.getString("sigla", id.toUpperCase()),
                    new LinkedHashSet<>(section.getStringList("jurisdiction_over"))));
        }
    }

    public Optional<Corps> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<Corps> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }
}
