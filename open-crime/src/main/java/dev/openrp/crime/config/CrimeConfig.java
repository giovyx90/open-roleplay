package dev.openrp.crime.config;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Facade that aggregates the global settings and every config-driven catalogue, and reloads them all
 * from the plugin data folder. The auxiliary files ({@code goods.yml}, {@code syndicate.yml}, ...)
 * are written from the bundled defaults on first run and re-read on every reload, so an operator
 * reshapes the whole setting in plain YAML without touching code.
 */
public final class CrimeConfig {

    private static final List<String> FILES = List.of(
            "goods.yml", "syndicate.yml", "production.yml", "traffic.yml", "laundering.yml", "racket.yml");

    private final JavaPlugin plugin;
    private final CrimeSettings settings = new CrimeSettings();
    private final GoodsCatalog goods = new GoodsCatalog();
    private final Hierarchy hierarchy = new Hierarchy();
    private final ProductionCatalog production = new ProductionCatalog();
    private final RouteCatalog routes = new RouteCatalog();
    private final LaunderingMethods laundering = new LaunderingMethods();
    private final RacketConfig racket = new RacketConfig();

    public CrimeConfig(JavaPlugin plugin) {
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
        goods.load(read("goods.yml").getConfigurationSection("goods"));
        hierarchy.load(read("syndicate.yml"));
        production.load(read("production.yml"));
        routes.load(read("traffic.yml").getConfigurationSection("routes"));
        laundering.load(read("laundering.yml").getConfigurationSection("laundering_methods"));
        racket.load(read("racket.yml"));
    }

    private YamlConfiguration read(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    public CrimeSettings settings() {
        return settings;
    }

    public GoodsCatalog goods() {
        return goods;
    }

    public Hierarchy hierarchy() {
        return hierarchy;
    }

    public ProductionCatalog production() {
        return production;
    }

    public RouteCatalog routes() {
        return routes;
    }

    public LaunderingMethods laundering() {
        return laundering;
    }

    public RacketConfig racket() {
        return racket;
    }
}
