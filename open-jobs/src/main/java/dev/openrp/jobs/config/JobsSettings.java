package dev.openrp.jobs.config;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Global settings from {@code config.yml}: storage backend, session-abandon window, the global toggles
 * for cooperative and seasonal pay, which optional adapters to look for, and the fallback used to pay
 * when no economy adapter is present.
 */
public final class JobsSettings {

    /** What to do with a session payout when no economy adapter is available. */
    public enum NoEconomyPayout {
        INVENTORY, CHEST, NONE;

        static NoEconomyPayout fromString(String raw) {
            if (raw == null) {
                return INVENTORY;
            }
            try {
                return NoEconomyPayout.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                return INVENTORY;
            }
        }
    }

    private String storageAdapter = "yaml";
    private String storageFile = "jobs-data.yml";

    private int sessionAbandonedMinutes = 10;
    private boolean payoutPartialAbandoned = true;
    private boolean cooperativeEnabled = true;
    private boolean seasonalEnabled;
    private boolean requireRegionBackend;

    private boolean adapterEconomy = true;
    private boolean adapterCompanies = true;
    private boolean adapterIdentity = true;
    private boolean adapterWorldguard = true;

    private NoEconomyPayout noEconomyPayout = NoEconomyPayout.INVENTORY;
    private String chestRegion = "";

    private boolean debug;

    public void load(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        ConfigurationSection general = config.getConfigurationSection("general");
        if (general != null) {
            sessionAbandonedMinutes = Math.max(1, general.getInt("session_abandoned_minutes", 10));
            payoutPartialAbandoned = general.getBoolean("payout_partial_abandoned", true);
            cooperativeEnabled = general.getBoolean("cooperative_enabled", true);
            seasonalEnabled = general.getBoolean("seasonal_enabled", false);
            requireRegionBackend = general.getBoolean("require_region_backend", false);
        }

        ConfigurationSection adapters = config.getConfigurationSection("adapters");
        if (adapters != null) {
            adapterEconomy = adapters.getBoolean("economy", true);
            adapterCompanies = adapters.getBoolean("companies", true);
            adapterIdentity = adapters.getBoolean("identity", true);
            adapterWorldguard = adapters.getBoolean("worldguard", true);
        }

        ConfigurationSection storage = config.getConfigurationSection("storage");
        if (storage != null) {
            storageAdapter = storage.getString("adapter", "yaml");
            storageFile = storage.getString("file", "jobs-data.yml");
        }

        ConfigurationSection fallback = config.getConfigurationSection("fallback");
        if (fallback != null) {
            noEconomyPayout = NoEconomyPayout.fromString(fallback.getString("no_economy_payout", "inventory"));
            chestRegion = fallback.getString("chest_region", "");
        }

        debug = config.getBoolean("debug", false);
    }

    public String storageAdapter() {
        return storageAdapter;
    }

    public String storageFile() {
        return storageFile;
    }

    public int sessionAbandonedMinutes() {
        return sessionAbandonedMinutes;
    }

    public long sessionAbandonedMillis() {
        return sessionAbandonedMinutes * 60_000L;
    }

    public boolean payoutPartialAbandoned() {
        return payoutPartialAbandoned;
    }

    public boolean cooperativeEnabled() {
        return cooperativeEnabled;
    }

    public boolean seasonalEnabled() {
        return seasonalEnabled;
    }

    public boolean requireRegionBackend() {
        return requireRegionBackend;
    }

    public boolean adapterEconomy() {
        return adapterEconomy;
    }

    public boolean adapterCompanies() {
        return adapterCompanies;
    }

    public boolean adapterIdentity() {
        return adapterIdentity;
    }

    public boolean adapterWorldguard() {
        return adapterWorldguard;
    }

    public NoEconomyPayout noEconomyPayout() {
        return noEconomyPayout;
    }

    public String chestRegion() {
        return chestRegion;
    }

    public boolean debug() {
        return debug;
    }
}
