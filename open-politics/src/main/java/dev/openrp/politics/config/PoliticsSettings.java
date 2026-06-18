package dev.openrp.politics.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Global settings from {@code config.yml}: module toggles, the active storage adapter, the act id
 * pattern, registry options and election behaviour. Day/hour durations configured elsewhere are turned
 * into real milliseconds here through the single {@link #timeScale() time scale}.
 */
public final class PoliticsSettings {

    private boolean moduleCharges = true;
    private boolean moduleGovernment = true;
    private boolean moduleActs = true;
    private boolean moduleLaws = true;

    private String storageAdapter = "yaml";
    private String storageFile = "politics-data.yml";

    private double timeScale = 1.0;

    private String actIdPattern = "{anno}/{numero}/{sigla}";
    private boolean actPhysicalBook = true;

    private boolean lawsPublicRegistry = true;
    private boolean lawsHistoricalArchive = true;
    private boolean lawsLinkOpenFdo = true;

    private boolean electionsAnnounceOpening = true;
    private boolean electionsAnnounceResults = true;
    private boolean electionsAnonymousVoting = true;
    private int electionsCheckIntervalMinutes = 5;

    private boolean debug;

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        ConfigurationSection modules = config.getConfigurationSection("modules");
        if (modules != null) {
            moduleCharges = modules.getBoolean("charges", true);
            moduleGovernment = modules.getBoolean("government", true);
            moduleActs = modules.getBoolean("acts", true);
            moduleLaws = modules.getBoolean("laws", true);
        }

        ConfigurationSection adapters = config.getConfigurationSection("adapters");
        if (adapters != null) {
            storageAdapter = adapters.getString("storage", "yaml");
        }
        ConfigurationSection storage = config.getConfigurationSection("storage");
        if (storage != null) {
            storageFile = storage.getString("file", "politics-data.yml");
        }

        ConfigurationSection time = config.getConfigurationSection("time");
        if (time != null) {
            timeScale = Math.max(0.01, time.getDouble("scale", 1.0));
        }

        ConfigurationSection acts = config.getConfigurationSection("acts");
        if (acts != null) {
            actIdPattern = acts.getString("id_pattern", "{anno}/{numero}/{sigla}");
            actPhysicalBook = acts.getBoolean("physical_book", true);
        }

        ConfigurationSection laws = config.getConfigurationSection("laws");
        if (laws != null) {
            lawsPublicRegistry = laws.getBoolean("public_registry", true);
            lawsHistoricalArchive = laws.getBoolean("historical_archive", true);
            lawsLinkOpenFdo = laws.getBoolean("link_openfdo", true);
        }

        ConfigurationSection elections = config.getConfigurationSection("elections");
        if (elections != null) {
            electionsAnnounceOpening = elections.getBoolean("announce_opening", true);
            electionsAnnounceResults = elections.getBoolean("announce_results", true);
            electionsAnonymousVoting = elections.getBoolean("anonymous_voting", true);
            electionsCheckIntervalMinutes = Math.max(1, elections.getInt("check_interval_minutes", 5));
        }

        debug = config.getBoolean("debug", false);
    }

    // --- module toggles ----------------------------------------------------------------------

    public boolean moduleCharges() {
        return moduleCharges;
    }

    public boolean moduleGovernment() {
        return moduleGovernment;
    }

    public boolean moduleActs() {
        return moduleActs;
    }

    public boolean moduleLaws() {
        return moduleLaws;
    }

    // --- storage -----------------------------------------------------------------------------

    public String storageAdapter() {
        return storageAdapter;
    }

    public String storageFile() {
        return storageFile;
    }

    // --- time --------------------------------------------------------------------------------

    public double timeScale() {
        return timeScale;
    }

    public long realMillisFromHours(double hours) {
        return (long) (Math.max(0.0, hours) * 3_600_000L * timeScale);
    }

    public long realMillisFromDays(double days) {
        return realMillisFromHours(days * 24.0);
    }

    // --- acts --------------------------------------------------------------------------------

    public String actIdPattern() {
        return actIdPattern;
    }

    public boolean actPhysicalBook() {
        return actPhysicalBook;
    }

    // --- laws --------------------------------------------------------------------------------

    public boolean lawsPublicRegistry() {
        return lawsPublicRegistry;
    }

    public boolean lawsHistoricalArchive() {
        return lawsHistoricalArchive;
    }

    public boolean lawsLinkOpenFdo() {
        return lawsLinkOpenFdo;
    }

    // --- elections ---------------------------------------------------------------------------

    public boolean electionsAnnounceOpening() {
        return electionsAnnounceOpening;
    }

    public boolean electionsAnnounceResults() {
        return electionsAnnounceResults;
    }

    public boolean electionsAnonymousVoting() {
        return electionsAnonymousVoting;
    }

    public int electionsCheckIntervalMinutes() {
        return electionsCheckIntervalMinutes;
    }

    public boolean debug() {
        return debug;
    }
}
