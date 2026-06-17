package dev.openrp.crime.config;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The catalogue of illegal goods, loaded from {@code goods.yml}. The core treats every good as an
 * opaque id with a danger level and a street value; all the flavour (name, category, item look) is
 * config data parsed here and nowhere else.
 */
public final class GoodsCatalog {

    private final Map<String, Good> byId = new LinkedHashMap<>();

    public void load(ConfigurationSection section) {
        byId.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection good = section.getConfigurationSection(id);
            if (good == null) {
                continue;
            }
            List<String> lore = good.getStringList("item_lore");
            byId.put(id, new Good(
                    id,
                    good.getString("display_name", id),
                    good.getString("category", "generic"),
                    good.getInt("danger_level", 1),
                    good.getLong("street_value", 0L),
                    good.getString("item_material", "PAPER"),
                    good.getInt("item_custom_model_data", 0),
                    good.getString("item_name", good.getString("display_name", id)),
                    lore));
        }
    }

    public Optional<Good> get(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id);
    }

    public Collection<Good> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }
}
