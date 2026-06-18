package dev.openrp.politics.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The catalogue of governments, loaded from {@code governments.yml}. Pure config: the core never knows
 * what a government <em>is</em> narratively, only that it owns charges and has a default mechanism.
 */
public final class GovernmentCatalog {

    private final Map<String, Government> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection government = section.getConfigurationSection(id);
            if (government == null) {
                continue;
            }
            byId.put(id, new Government(
                    id,
                    government.getString("display_name", id),
                    government.getString("sigla", id),
                    government.getBoolean("active", true),
                    government.getString("assignment_mechanism", AssignmentMechanism.APPOINTMENT),
                    government.getStringList("charges")));
        }
    }

    public Optional<Government> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<Government> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
