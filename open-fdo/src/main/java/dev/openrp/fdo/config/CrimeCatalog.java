package dev.openrp.fdo.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/** Holds the configured charge catalogue ({@code crimes.yml}). */
public final class CrimeCatalog {

    private final Map<String, Crime> byId = new LinkedHashMap<>();

    /** Replaces the catalogue from the {@code crimes} section of {@code crimes.yml}. */
    public void load(ConfigurationSection root) {
        byId.clear();
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section != null) {
                byId.put(id, new Crime(id, section.getString("label", id), section.getInt("gravity", 1)));
            } else {
                // Allow the compact "id: label" form too.
                byId.put(id, new Crime(id, root.getString(id, id), 1));
            }
        }
    }

    public Optional<Crime> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<Crime> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
