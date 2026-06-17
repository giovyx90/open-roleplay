package dev.openrp.jobs.config;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Facade that aggregates the global settings and every config-driven catalogue (jobs, location types,
 * progression ladder) and reloads them from the plugin data folder. The auxiliary files are written
 * from the bundled defaults on first run and re-read on every reload, so an operator reshapes the whole
 * setting in plain YAML without touching code.
 */
public final class JobsConfig {

    private static final List<String> FILES = List.of("jobs.yml", "location_types.yml", "progression.yml");

    private final JavaPlugin plugin;
    private final JobsSettings settings = new JobsSettings();
    private final JobsRegistry jobs = new JobsRegistry();
    private final LocationTypeCatalog locationTypes = new LocationTypeCatalog();
    private final ProgressionLadder progression = new ProgressionLadder();

    public JobsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        for (String file : FILES) {
            if (!new File(plugin.getDataFolder(), file).exists()) {
                plugin.saveResource(file, false);
            }
        }
        settings.load(plugin.getConfig());
        jobs.load(read("jobs.yml").getConfigurationSection("jobs"));
        locationTypes.load(read("location_types.yml").getConfigurationSection("location_types"));
        progression.load(read("progression.yml").getConfigurationSection("progression"));
    }

    private YamlConfiguration read(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    public JobsSettings settings() {
        return settings;
    }

    public JobsRegistry jobs() {
        return jobs;
    }

    public LocationTypeCatalog locationTypes() {
        return locationTypes;
    }

    public ProgressionLadder progression() {
        return progression;
    }
}
