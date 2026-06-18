package dev.openrp.politics.config;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Facade that aggregates the global settings and every config-driven catalogue, and reloads them all
 * from the plugin data folder. The auxiliary files ({@code governments.yml}, {@code charges.yml}, ...)
 * are written from the bundled defaults on first run and re-read on every reload, so an operator
 * reshapes the whole institutional setting in plain YAML without touching code.
 */
public final class PoliticsConfig {

    private static final List<String> FILES =
            List.of("governments.yml", "charges.yml", "act_types.yml", "law_categories.yml");

    private final JavaPlugin plugin;
    private final PoliticsSettings settings = new PoliticsSettings();
    private final GovernmentCatalog governments = new GovernmentCatalog();
    private final ChargeCatalog charges = new ChargeCatalog();
    private final ActTypeCatalog actTypes = new ActTypeCatalog();
    private final LawCategoryCatalog lawCategories = new LawCategoryCatalog();

    public PoliticsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Saves any missing default files, then reloads settings and every catalogue. */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        for (String file : FILES) {
            if (!new File(plugin.getDataFolder(), file).exists()) {
                plugin.saveResource(file, false);
            }
        }
        settings.load(plugin.getConfig());
        governments.load(read("governments.yml").getConfigurationSection("governments"));
        charges.load(read("charges.yml").getConfigurationSection("charges"), governments);
        actTypes.load(read("act_types.yml").getConfigurationSection("act_types"));
        lawCategories.load(read("law_categories.yml").getConfigurationSection("law_categories"));
    }

    private YamlConfiguration read(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    public PoliticsSettings settings() {
        return settings;
    }

    public GovernmentCatalog governments() {
        return governments;
    }

    public ChargeCatalog charges() {
        return charges;
    }

    public ActTypeCatalog actTypes() {
        return actTypes;
    }

    public LawCategoryCatalog lawCategories() {
        return lawCategories;
    }
}
