package dev.openrp.jobs;

import java.io.File;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import dev.openrp.jobs.adapter.AdapterRegistry;
import dev.openrp.jobs.adapter.CompanyEmploymentAdapter;
import dev.openrp.jobs.adapter.EconomyAdapter;
import dev.openrp.jobs.adapter.IdentityAdapter;
import dev.openrp.jobs.adapter.RegionAdapter;
import dev.openrp.jobs.adapter.StorageAdapter;
import dev.openrp.jobs.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.jobs.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.jobs.adapter.defaults.ChunkRegionAdapter;
import dev.openrp.jobs.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.jobs.adapter.defaults.NoopCompanyEmploymentAdapter;
import dev.openrp.jobs.adapter.defaults.NoopEconomyAdapter;
import dev.openrp.jobs.adapter.defaults.NoopIdentityAdapter;
import dev.openrp.jobs.adapter.defaults.YamlStorageAdapter;
import dev.openrp.jobs.api.OpenJobsApi;
import dev.openrp.jobs.api.OpenJobsApiProvider;
import dev.openrp.jobs.command.LavoroCommand;
import dev.openrp.jobs.config.JobsConfig;
import dev.openrp.jobs.config.JobsSettings;
import dev.openrp.jobs.core.LicenseManager;
import dev.openrp.jobs.core.LocationManager;
import dev.openrp.jobs.core.PaymentService;
import dev.openrp.jobs.core.ProgressionService;
import dev.openrp.jobs.core.RecordManager;
import dev.openrp.jobs.core.SessionManager;
import dev.openrp.jobs.integration.OpenCoreModuleRegistration;
import dev.openrp.jobs.listener.ActivityListener;
import dev.openrp.jobs.listener.MovementListener;
import dev.openrp.jobs.message.JobMessages;
import dev.openrp.jobs.message.LanguageService;

/**
 * Open Jobs entry point. Wires the config, message layer, adapter set and core services together. The
 * plugin is fully standalone: OpenCore is optional, and the world-facing adapters (economy, company,
 * identity, region) start as bundled defaults and are replaced at runtime by whatever bridge plugin
 * registers a real one with the ServicesManager. Adapters are created once on enable so integrations
 * swapped in by other plugins survive a reload; only config, messages and data are re-read.
 *
 * <p>RP First: the plugin does not pay the time, it pays the activity actually done. A work session
 * tracks concrete activity (blocks broken, fish caught, items crafted), never minutes idled.
 */
public final class OpenJobsPlugin extends JavaPlugin {

    private static final long ABANDON_CHECK_TICKS = 1_200L; // every ~60s

    private final AdapterRegistry adapters = new AdapterRegistry();

    private JobsConfig config;
    private LanguageService languageService;
    private JobMessages messages;

    private LocationManager locations;
    private RecordManager records;
    private LicenseManager licenses;
    private ProgressionService progression;
    private PaymentService payment;
    private SessionManager sessions;

    private OpenJobsApiProvider apiProvider;
    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;
    private BukkitTask abandonTask;

    @Override
    public void onEnable() {
        this.config = new JobsConfig(this);
        config.reload();
        saveMissingResources();

        this.languageService = new LanguageService(this);
        languageService.reload();
        this.messages = new JobMessages(this, languageService);
        messages.reload();

        setupAdapters();

        this.locations = new LocationManager(this);
        locations.loadAll();
        this.records = new RecordManager(this);
        records.loadAll();
        this.licenses = new LicenseManager(this);
        licenses.loadAll();
        this.progression = new ProgressionService(config.progression());
        this.payment = new PaymentService();
        this.sessions = new SessionManager(this);
        sessions.loadAll();

        this.apiProvider = new OpenJobsApiProvider(this);
        getServer().getServicesManager().register(OpenJobsApi.class, apiProvider, this, ServicePriority.Normal);

        registerCommands();
        registerListeners();
        scheduleTasks();
        setupIntegrations();

        getLogger().info("[OpenJobs] Enabled. " + config.jobs().all().size() + " job(s), "
                + locations.all().size() + " location(s).");
    }

    @Override
    public void onDisable() {
        if (abandonTask != null) {
            abandonTask.cancel();
            abandonTask = null;
        }
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenJobsApi.class, apiProvider);
        }
        if (adapters.storage() != null) {
            adapters.storage().flush();
            adapters.storage().close();
        }
    }

    /** Re-reads config, language, messages and persisted data. Adapters are intentionally preserved. */
    public void reloadAll() {
        config.reload();
        languageService.reload();
        messages.reload();
        // The progression service holds the same ladder instance, which config.reload() refreshed.
        locations.loadAll();
        records.loadAll();
        licenses.loadAll();
        sessions.loadAll();
    }

    private void setupAdapters() {
        StorageAdapter storage = createStorageAdapter(config.settings().storageAdapter());
        storage.init();
        adapters.setStorage(storage);
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setRegion(new ChunkRegionAdapter());
        adapters.setEconomy(new NoopEconomyAdapter());
        adapters.setCompany(new NoopCompanyEmploymentAdapter());
        adapters.setIdentity(new NoopIdentityAdapter());
        discoverOptionalAdapters();
    }

    private StorageAdapter createStorageAdapter(String id) {
        if ("memory".equalsIgnoreCase(id)) {
            return new MemoryStorageAdapter();
        }
        File file = new File(getDataFolder(), config.settings().storageFile());
        return new YamlStorageAdapter(file, getLogger());
    }

    /** Picks up world-facing adapters other plugins have registered with the ServicesManager. */
    private void discoverOptionalAdapters() {
        JobsSettings settings = config.settings();
        if (settings.adapterWorldguard()) {
            discover(RegionAdapter.class).ifPresent(adapter -> {
                adapters.setRegion(adapter);
                getLogger().info("[OpenJobs] Region adapter active: " + adapter.id());
            });
        }
        if (settings.adapterEconomy()) {
            discover(EconomyAdapter.class).ifPresent(adapter -> {
                adapters.setEconomy(adapter);
                getLogger().info("[OpenJobs] Economy adapter active: " + adapter.id());
            });
        }
        if (settings.adapterCompanies()) {
            discover(CompanyEmploymentAdapter.class).ifPresent(adapter -> {
                adapters.setCompany(adapter);
                getLogger().info("[OpenJobs] Company adapter active: " + adapter.id());
            });
        }
        if (settings.adapterIdentity()) {
            discover(IdentityAdapter.class).ifPresent(adapter -> {
                adapters.setIdentity(adapter);
                getLogger().info("[OpenJobs] Identity adapter active: " + adapter.id());
            });
        }
    }

    private <T> Optional<T> discover(Class<T> type) {
        RegisteredServiceProvider<T> registration = getServer().getServicesManager().getRegistration(type);
        return registration == null ? Optional.empty() : Optional.ofNullable(registration.getProvider());
    }

    private void registerCommands() {
        setExecutor("lavoro", new LavoroCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ActivityListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
    }

    private void scheduleTasks() {
        abandonTask = getServer().getScheduler().runTaskTimer(
                this, sessions::checkAbandoned, ABANDON_CHECK_TICKS, ABANDON_CHECK_TICKS);
    }

    private void setupIntegrations() {
        this.openCoreRegistration = new OpenCoreModuleRegistration(this);
        this.registeredInOpenCore = openCoreRegistration.register();
    }

    private void saveMissingResources() {
        for (String resource : List.of("messages_en.yml", "messages_it.yml")) {
            if (!new File(getDataFolder(), resource).exists()) {
                saveResource(resource, false);
            }
        }
    }

    /** Wires a command executor declared in {@code plugin.yml}; warns (never crashes) if missing. */
    public void setExecutor(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("[OpenJobs] Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    // --- accessors ---------------------------------------------------------------------------

    public AdapterRegistry adapters() {
        return adapters;
    }

    public JobsConfig config() {
        return config;
    }

    public JobsSettings settings() {
        return config.settings();
    }

    public JobMessages messages() {
        return messages;
    }

    public LocationManager locations() {
        return locations;
    }

    public RecordManager records() {
        return records;
    }

    public LicenseManager licenses() {
        return licenses;
    }

    public ProgressionService progression() {
        return progression;
    }

    public PaymentService payment() {
        return payment;
    }

    public SessionManager sessions() {
        return sessions;
    }

    public OpenJobsApi api() {
        return apiProvider;
    }
}
