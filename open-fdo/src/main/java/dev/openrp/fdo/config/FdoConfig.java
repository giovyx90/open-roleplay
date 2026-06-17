package dev.openrp.fdo.config;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Facade that aggregates the global settings and every config-driven registry, and reloads them all
 * from the plugin data folder. The auxiliary files ({@code corps.yml}, {@code ranks.yml}, ...) are
 * written from the bundled defaults on first run and re-read on every reload, so an operator edits
 * plain YAML and never touches code to reshape the whole setting.
 */
public final class FdoConfig {

    private static final List<String> FILES = List.of(
            "corps.yml", "ranks.yml", "acts.yml", "crimes.yml", "wanted.yml", "commands.yml");

    private final JavaPlugin plugin;
    private final FdoSettings settings = new FdoSettings();
    private final CorpsRegistry corps = new CorpsRegistry();
    private final RankRegistry ranks = new RankRegistry();
    private final ActCatalog acts;
    private final CrimeCatalog crimes = new CrimeCatalog();
    private final WantedLevels wanted = new WantedLevels();
    private YamlConfiguration commands = new YamlConfiguration();

    public FdoConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.acts = new ActCatalog(plugin.getLogger());
    }

    /** Saves any missing default files, then reloads settings and every registry. */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        for (String file : FILES) {
            if (!new File(plugin.getDataFolder(), file).exists()) {
                plugin.saveResource(file, false);
            }
        }
        settings.load(plugin.getConfig());
        corps.load(read("corps.yml").getConfigurationSection("corps"));
        ranks.load(read("ranks.yml").getConfigurationSection("ranks"));
        acts.load(read("acts.yml").getConfigurationSection("acts"));
        crimes.load(read("crimes.yml").getConfigurationSection("crimes"));
        wanted.load(read("wanted.yml"));
        commands = read("commands.yml");
    }

    private YamlConfiguration read(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    public FdoSettings settings() {
        return settings;
    }

    public CorpsRegistry corps() {
        return corps;
    }

    public RankRegistry ranks() {
        return ranks;
    }

    public ActCatalog acts() {
        return acts;
    }

    public CrimeCatalog crimes() {
        return crimes;
    }

    public WantedLevels wanted() {
        return wanted;
    }

    public YamlConfiguration commands() {
        return commands;
    }
}
