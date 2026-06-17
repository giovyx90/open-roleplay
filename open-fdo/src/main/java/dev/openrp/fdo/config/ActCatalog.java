package dev.openrp.fdo.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.fdo.capability.Capability;

/**
 * Holds the configured act catalogue ({@code acts.yml}). An act with an unknown capability is
 * skipped with a warning rather than crashing startup. {@code custody_hours} accepts either a number
 * or the keyword {@code default}, which maps to {@code -1} (resolved against the global default by
 * the act service).
 */
public final class ActCatalog {

    private final Map<String, ActDefinition> byId = new LinkedHashMap<>();
    private final Logger logger;

    public ActCatalog(Logger logger) {
        this.logger = logger;
    }

    /** Replaces the catalogue from the {@code acts} section of {@code acts.yml}. */
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
            Optional<Capability> capability = Capability.fromString(section.getString("capability"));
            if (capability.isEmpty()) {
                logger.warning("[OpenFDO] Skipping act '" + id + "': unknown capability '"
                        + section.getString("capability") + "'.");
                continue;
            }
            byId.put(id, new ActDefinition(
                    id,
                    section.getString("display_name", id),
                    capability.get(),
                    emptyToNull(section.getString("requires_adapter")),
                    section.getBoolean("opens_dossier", false),
                    section.getBoolean("starts_custody", false),
                    readCustodyHours(section),
                    section.getBoolean("seizes_evidence", false),
                    section.getBoolean("flags_wanted", false),
                    section.getInt("wanted_level", 1),
                    section.getBoolean("issues_fine", false),
                    section.getString("book_template", id),
                    section.getString("icon", "PAPER")));
        }
    }

    private int readCustodyHours(ConfigurationSection section) {
        Object raw = section.get("custody_hours");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        // "default", missing, or any non-numeric value -> use the global default.
        return -1;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public Optional<ActDefinition> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public Collection<ActDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
