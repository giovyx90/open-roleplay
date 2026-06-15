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
import dev.openrp.vending.model.ProductDefinition;

/** Loads and indexes sellable product definitions from products.yml. */
public final class ProductRegistry {

    private final Logger logger;
    private final Map<String, ProductDefinition> products = new LinkedHashMap<>();

    public ProductRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(File file) {
        products.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                Material material = Material.matchMaterial(section.getString("material", "PAPER"));
                ProductDefinition product = new ProductDefinition(
                        key,
                        section.getString("display-name", key),
                        material == null ? Material.PAPER : material,
                        section.getInt("amount", 1),
                        section.getDouble("price", 0.0),
                        section.getInt("max-stock", 16),
                        section.getInt("custom-model-data", 0),
                        section.getStringList("lore"));
                products.put(product.id(), product);
            } catch (RuntimeException exception) {
                logger.warning("[OpenVendingMachines] Failed to load product '" + key + "': " + exception.getMessage());
            }
        }
        logger.info("[OpenVendingMachines] Loaded " + products.size() + " product(s).");
    }

    public ProductDefinition get(String id) {
        return id == null ? null : products.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return get(id) != null;
    }

    public Collection<ProductDefinition> all() {
        return products.values();
    }

    public Set<String> ids() {
        return products.keySet();
    }
}
