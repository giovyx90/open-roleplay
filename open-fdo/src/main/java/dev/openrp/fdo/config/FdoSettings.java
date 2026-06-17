package dev.openrp.fdo.config;

import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over {@code config.yml} - the global, setting-agnostic knobs. Everything that names a
 * corps, rank, crime or wanted level lives in the dedicated files, not here. Reloadable: call
 * {@link #load} again after re-reading the config.
 */
public final class FdoSettings {

    private String storageAdapter = "yaml";
    private String storageFile = "fdo-data.yml";
    private String loggingAdapter = "file";
    private String loggingFile = "fdo-audit.log";
    private boolean internalDutyEnabled = true;
    private long defaultCustodyHours = 48L;
    private long detainHours = 6L;
    private String dossierIdPattern = "{anno}/{numero}/{sigla_corpo}";
    private long timeScaleSecondsPerHour = 60L;
    private String badgeMaterial = "PAPER";
    private String bookMaterial = "WRITABLE_BOOK";
    private int defaultSecurityLevel = 1;
    private boolean debug;

    public void load(FileConfiguration config) {
        this.storageAdapter = config.getString("adapters.storage", "yaml");
        this.storageFile = config.getString("storage.file", "fdo-data.yml");
        this.loggingAdapter = config.getString("adapters.logging", "file");
        this.loggingFile = config.getString("logging.file", "fdo-audit.log");
        this.internalDutyEnabled = config.getBoolean("duty.internal-fallback", true);
        this.defaultCustodyHours = config.getLong("custody.default-hours", 48L);
        this.detainHours = config.getLong("custody.detain-hours", 6L);
        this.dossierIdPattern = config.getString("dossier.id-pattern", "{anno}/{numero}/{sigla_corpo}");
        this.timeScaleSecondsPerHour = Math.max(1L, config.getLong("time.seconds-per-ig-hour", 60L));
        this.badgeMaterial = config.getString("items.badge-material", "PAPER");
        this.bookMaterial = config.getString("items.book-material", "WRITABLE_BOOK");
        this.defaultSecurityLevel = config.getInt("detention.default-security-level", 1);
        this.debug = config.getBoolean("debug", false);
    }

    public String storageAdapter() {
        return storageAdapter;
    }

    public String storageFile() {
        return storageFile;
    }

    public String loggingAdapter() {
        return loggingAdapter == null ? "file" : loggingAdapter.toLowerCase(Locale.ROOT);
    }

    public String loggingFile() {
        return loggingFile;
    }

    public boolean internalDutyEnabled() {
        return internalDutyEnabled;
    }

    public long defaultCustodyHours() {
        return defaultCustodyHours;
    }

    public long detainHours() {
        return detainHours;
    }

    public String dossierIdPattern() {
        return dossierIdPattern;
    }

    /**
     * Conversion factor from one in-game hour to real seconds, used to turn an abstract custody or
     * sentence length expressed in IG hours into a wall-clock timer. Default 60 (one IG hour = one
     * real minute). Never zero.
     */
    public long timeScaleSecondsPerHour() {
        return timeScaleSecondsPerHour;
    }

    public String badgeMaterial() {
        return badgeMaterial;
    }

    public String bookMaterial() {
        return bookMaterial;
    }

    public int defaultSecurityLevel() {
        return defaultSecurityLevel;
    }

    public boolean debug() {
        return debug;
    }
}
