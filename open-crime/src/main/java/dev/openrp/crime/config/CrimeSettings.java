package dev.openrp.crime.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Global settings from {@code config.yml}: module toggles, the active storage adapter, discovery
 * windows, and the per-subsystem switches. Hours/minutes/days configured elsewhere are turned into
 * real milliseconds here through the single {@link #timeScale() time scale}.
 */
public final class CrimeSettings {

    private boolean moduleSyndicate = true;
    private boolean moduleProduction = true;
    private boolean moduleTraffic = true;
    private boolean moduleLaundering = true;
    private boolean moduleRacket = true;

    private String storageAdapter = "yaml";
    private String storageFile = "crime-data.yml";

    private double timeScale = 1.0;

    private int denunciaRadius = 12;
    private int denunciaEventWindowMinutes = 120;
    private int arrestEventWindowMinutes = 360;
    private boolean informantProtection;

    private boolean territoryRequireWorldguard = true;
    private boolean territoryContestedNotifyMembers = true;

    private boolean productionAllowPauseOnLeave = true;

    private boolean trafficGoodsDroppableOnDeath;
    private int trafficEscortMax = 4;

    private boolean launderingRequiresBankAdapter = true;

    private boolean racketRequiresCompaniesAdapter = true;
    private boolean racketPaymentAuto = true;

    private boolean debug;

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        ConfigurationSection modules = config.getConfigurationSection("modules");
        if (modules != null) {
            moduleSyndicate = modules.getBoolean("syndicate", true);
            moduleProduction = modules.getBoolean("production", true);
            moduleTraffic = modules.getBoolean("traffic", true);
            moduleLaundering = modules.getBoolean("laundering", true);
            moduleRacket = modules.getBoolean("racket", true);
        }

        ConfigurationSection adapters = config.getConfigurationSection("adapters");
        if (adapters != null) {
            storageAdapter = adapters.getString("storage", "yaml");
        }
        ConfigurationSection storage = config.getConfigurationSection("storage");
        if (storage != null) {
            storageFile = storage.getString("file", "crime-data.yml");
        }

        ConfigurationSection time = config.getConfigurationSection("time");
        if (time != null) {
            timeScale = Math.max(0.01, time.getDouble("scale", 1.0));
        }

        ConfigurationSection discovery = config.getConfigurationSection("discovery");
        if (discovery != null) {
            denunciaRadius = Math.max(1, discovery.getInt("denuncia_radius", 12));
            denunciaEventWindowMinutes = Math.max(1, discovery.getInt("denuncia_event_window", 120));
            arrestEventWindowMinutes = Math.max(1, discovery.getInt("arrest_event_window", 360));
            informantProtection = discovery.getBoolean("informant_protection", false);
        }

        ConfigurationSection territory = config.getConfigurationSection("territory");
        if (territory != null) {
            territoryRequireWorldguard = territory.getBoolean("require_worldguard", true);
            territoryContestedNotifyMembers = territory.getBoolean("contested_notify_members", true);
        }

        ConfigurationSection production = config.getConfigurationSection("production");
        if (production != null) {
            productionAllowPauseOnLeave = production.getBoolean("allow_pause_on_leave", true);
            if (production.isSet("time_scale")) {
                timeScale = Math.max(0.01, production.getDouble("time_scale", timeScale));
            }
        }

        ConfigurationSection traffic = config.getConfigurationSection("traffic");
        if (traffic != null) {
            trafficGoodsDroppableOnDeath = traffic.getBoolean("goods_droppable_on_death", false);
            trafficEscortMax = Math.max(0, traffic.getInt("escort_max", 4));
        }

        ConfigurationSection laundering = config.getConfigurationSection("laundering");
        if (laundering != null) {
            launderingRequiresBankAdapter = laundering.getBoolean("requires_bank_adapter", true);
        }

        ConfigurationSection racket = config.getConfigurationSection("racket");
        if (racket != null) {
            racketRequiresCompaniesAdapter = racket.getBoolean("requires_companies_adapter", true);
            racketPaymentAuto = racket.getBoolean("payment_auto", true);
        }

        debug = config.getBoolean("debug", false);
    }

    // --- module toggles ----------------------------------------------------------------------

    public boolean moduleSyndicate() {
        return moduleSyndicate;
    }

    public boolean moduleProduction() {
        return moduleProduction;
    }

    public boolean moduleTraffic() {
        return moduleTraffic;
    }

    public boolean moduleLaundering() {
        return moduleLaundering;
    }

    public boolean moduleRacket() {
        return moduleRacket;
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

    public long realMillisFromMinutes(double minutes) {
        return (long) (Math.max(0.0, minutes) * 60_000L * timeScale);
    }

    public long realMillisFromHours(double hours) {
        return realMillisFromMinutes(hours * 60.0);
    }

    public long realMillisFromDays(double days) {
        return realMillisFromHours(days * 24.0);
    }

    // --- discovery ---------------------------------------------------------------------------

    public int denunciaRadius() {
        return denunciaRadius;
    }

    public long denunciaEventWindowMillis() {
        return denunciaEventWindowMinutes * 60_000L;
    }

    public long arrestEventWindowMillis() {
        return arrestEventWindowMinutes * 60_000L;
    }

    public boolean informantProtection() {
        return informantProtection;
    }

    // --- territory ---------------------------------------------------------------------------

    public boolean territoryRequireWorldguard() {
        return territoryRequireWorldguard;
    }

    public boolean territoryContestedNotifyMembers() {
        return territoryContestedNotifyMembers;
    }

    // --- production --------------------------------------------------------------------------

    public boolean productionAllowPauseOnLeave() {
        return productionAllowPauseOnLeave;
    }

    // --- traffic -----------------------------------------------------------------------------

    public boolean trafficGoodsDroppableOnDeath() {
        return trafficGoodsDroppableOnDeath;
    }

    public int trafficEscortMax() {
        return trafficEscortMax;
    }

    // --- laundering --------------------------------------------------------------------------

    public boolean launderingRequiresBankAdapter() {
        return launderingRequiresBankAdapter;
    }

    // --- racket ------------------------------------------------------------------------------

    public boolean racketRequiresCompaniesAdapter() {
        return racketRequiresCompaniesAdapter;
    }

    public boolean racketPaymentAuto() {
        return racketPaymentAuto;
    }

    public boolean debug() {
        return debug;
    }
}
