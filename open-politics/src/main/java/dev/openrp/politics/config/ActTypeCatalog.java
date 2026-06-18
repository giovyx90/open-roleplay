package dev.openrp.politics.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.politics.capability.PoliticalCapability;

/** The catalogue of act types, loaded from {@code act_types.yml}. */
public final class ActTypeCatalog {

    private final Map<String, ActType> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection type = section.getConfigurationSection(id);
            if (type == null) {
                continue;
            }
            byId.put(id, new ActType(
                    id,
                    type.getString("display_name", id),
                    PoliticalCapability.fromString(type.getString("capability_required"))
                            .orElse(PoliticalCapability.SIGN_ACT),
                    type.getBoolean("can_become_law", false),
                    type.getBoolean("requires_vote", false),
                    type.getString("submit_to", ""),
                    type.getBoolean("veto_allowed", false),
                    PoliticalCapability.fromString(type.getString("veto_capability"))
                            .orElse(PoliticalCapability.VETO),
                    type.getInt("veto_window_hours", 24),
                    PoliticalCapability.fromString(type.getString("promulgated_by_capability"))
                            .orElse(PoliticalCapability.SIGN_LAW),
                    type.getString("law_category", "")));
        }
    }

    public Optional<ActType> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<ActType> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
