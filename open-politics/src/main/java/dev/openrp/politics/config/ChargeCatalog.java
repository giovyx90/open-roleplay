package dev.openrp.politics.config;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.politics.capability.PoliticalCapability;

/**
 * The catalogue of charges, loaded from {@code charges.yml}. The core treats each charge as an opaque
 * id with an authority level and a capability set; all flavour (name, mechanism) is config parsed here
 * and nowhere else. The default mechanism type is inherited from the owning government.
 */
public final class ChargeCatalog {

    private final Map<String, ChargeDef> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section, GovernmentCatalog governments) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection charge = section.getConfigurationSection(id);
            if (charge == null) {
                continue;
            }
            String governmentId = charge.getString("government_id", "");
            String defaultType = governments.get(governmentId)
                    .map(Government::defaultMechanism)
                    .orElse(AssignmentMechanism.APPOINTMENT);
            Set<PoliticalCapability> caps = EnumSet.noneOf(PoliticalCapability.class);
            for (String raw : charge.getStringList("capabilities")) {
                PoliticalCapability.fromString(raw).ifPresent(caps::add);
            }
            byId.put(id, new ChargeDef(
                    id,
                    charge.getString("display_name", id),
                    governmentId,
                    charge.getInt("authority_level", 0),
                    charge.getInt("max_holders", 1),
                    charge.getInt("term_duration_days", 0),
                    caps,
                    AssignmentMechanism.from(charge.getConfigurationSection("assignment_mechanism"), defaultType),
                    CollegiateConfig.from(charge.getConfigurationSection("collegiate"))));
        }
    }

    public Optional<ChargeDef> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<ChargeDef> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<ChargeDef> ofGovernment(String governmentId) {
        return byId.values().stream()
                .filter(charge -> charge.governmentId().equals(governmentId))
                .toList();
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
