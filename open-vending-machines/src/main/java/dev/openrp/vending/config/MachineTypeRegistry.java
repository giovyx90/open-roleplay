package dev.openrp.vending.config;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.vending.model.MachineType;

/** Loads and indexes machine type/model definitions from machines.yml. */
public final class MachineTypeRegistry {

    private final Logger logger;
    private final Map<String, MachineType> types = new LinkedHashMap<>();

    public MachineTypeRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(File file) {
        types.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                Material icon = Material.matchMaterial(section.getString("icon", "DROPPER"));
                MachineType type = new MachineType(
                        key,
                        section.getString("display-name", key),
                        icon == null ? Material.DROPPER : icon,
                        section.getInt("slots", 6),
                        section.getStringList("default-products"));
                types.put(type.id(), type);
            } catch (RuntimeException exception) {
                logger.warning("[OpenVendingMachines] Failed to load machine type '" + key + "': " + exception.getMessage());
            }
        }
        logger.info("[OpenVendingMachines] Loaded " + types.size() + " machine type(s).");
    }

    public MachineType get(String id) {
        return id == null ? null : types.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return get(id) != null;
    }

    public Collection<MachineType> all() {
        return types.values();
    }

    public Set<String> ids() {
        return types.keySet();
    }
}
