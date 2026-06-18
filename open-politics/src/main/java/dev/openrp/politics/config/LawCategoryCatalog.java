package dev.openrp.politics.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The catalogue of law categories, loaded from {@code law_categories.yml}. Pure labels: the core never
 * attaches behaviour to a category, it only files a law under that heading for the public registry.
 */
public final class LawCategoryCatalog {

    private final Map<String, String> displayNames = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        displayNames.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection category = section.getConfigurationSection(id);
            String name = category == null ? id : category.getString("display_name", id);
            displayNames.put(id, name);
        }
    }

    public boolean exists(String id) {
        return id != null && displayNames.containsKey(id);
    }

    /** Display name for a category, or the id itself when unknown (a free category is never fatal). */
    public String displayName(String id) {
        return displayNames.getOrDefault(id, id == null ? "" : id);
    }

    public List<String> ids() {
        return List.copyOf(displayNames.keySet());
    }
}
