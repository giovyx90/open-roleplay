package dev.openrp.fdo;

import java.io.File;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.adapter.DetentionAdapter;
import dev.openrp.fdo.adapter.EconomyAuditAdapter;
import dev.openrp.fdo.adapter.EvidenceSourceAdapter;
import dev.openrp.fdo.adapter.ExternalRecordAdapter;
import dev.openrp.fdo.adapter.LoggingAdapter;
import dev.openrp.fdo.adapter.RadioAdapter;
import dev.openrp.fdo.adapter.StorageAdapter;
import dev.openrp.fdo.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.fdo.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.fdo.adapter.defaults.ConsoleLoggingAdapter;
import dev.openrp.fdo.adapter.defaults.FileLoggingAdapter;
import dev.openrp.fdo.adapter.defaults.InternalDutyStatusAdapter;
import dev.openrp.fdo.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.fdo.adapter.defaults.NoopLoggingAdapter;
import dev.openrp.fdo.adapter.defaults.NoopRegionAdapter;
import dev.openrp.fdo.adapter.defaults.YamlStorageAdapter;
import dev.openrp.fdo.api.OpenFdoApi;
import dev.openrp.fdo.api.OpenFdoApiProvider;
import dev.openrp.fdo.command.AllertaCommand;
import dev.openrp.fdo.command.AttoCommand;
import dev.openrp.fdo.command.DetenzioneCommand;
import dev.openrp.fdo.command.FdoCommand;
import dev.openrp.fdo.command.RegistroCommand;
import dev.openrp.fdo.config.FdoConfig;
import dev.openrp.fdo.core.ActService;
import dev.openrp.fdo.core.AgentManager;
import dev.openrp.fdo.core.AlertManager;
import dev.openrp.fdo.core.Counters;
import dev.openrp.fdo.core.DetentionManager;
import dev.openrp.fdo.core.DossierManager;
import dev.openrp.fdo.core.DutyService;
import dev.openrp.fdo.core.EvidenceManager;
import dev.openrp.fdo.core.ServiceSheetService;
import dev.openrp.fdo.core.WantedManager;
import dev.openrp.fdo.gui.ActMenu;
import dev.openrp.fdo.gui.MenuListener;
import dev.openrp.fdo.integration.OpenCoreModuleRegistration;
import dev.openrp.fdo.item.ActBook;
import dev.openrp.fdo.item.Tesserino;
import dev.openrp.fdo.listener.ActBookListener;
import dev.openrp.fdo.message.FdoMessages;
import dev.openrp.fdo.message.LanguageService;

/**
 * Open FDO entry point. Wires the config, message layer, adapter set, the core managers/services, the
 * items and the public API together. The plugin is fully standalone: OpenCore is optional, and the
 * world-facing adapters (detention, economy audit, external records, radio, evidence source) are
 * discovered from the Bukkit ServicesManager when present and otherwise silently absent - the acts
 * that need them simply do not appear. Adapters are created once on enable so integrations swapped in
 * by other plugins survive a {@code /fdo reload}; only config, messages and data are re-read.
 */
public final class OpenFdoPlugin extends JavaPlugin {

    private final AdapterRegistry adapters = new AdapterRegistry();

    private FdoConfig config;
    private LanguageService languageService;
    private FdoMessages messages;
    private Counters counters;
    private AgentManager agents;
    private DossierManager dossiers;
    private EvidenceManager evidence;
    private WantedManager wanted;
    private AlertManager alerts;
    private DutyService duty;
    private ActService acts;
    private ServiceSheetService serviceSheets;
    private DetentionManager detention;
    private Tesserino badge;
    private ActBook actBook;
    private ActMenu menus;

    private OpenFdoApiProvider apiProvider;
    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;

    @Override
    public void onEnable() {
        this.config = new FdoConfig(this);
        config.reload();
        saveMissingResources();

        this.languageService = new LanguageService(this);
        languageService.reload();
        this.messages = new FdoMessages(this, languageService);
        messages.reload();

        setupAdapters();

        this.counters = new Counters(adapters);
        counters.loadAll();
        this.agents = new AgentManager(config, adapters, counters);
        agents.loadAll();

        adapters.setDuty(new InternalDutyStatusAdapter(agents::corpsOf));
        discoverOptionalAdapters();

        this.dossiers = new DossierManager(config, adapters, counters);
        dossiers.loadAll();
        this.evidence = new EvidenceManager(adapters, dossiers);
        evidence.loadAll();
        this.wanted = new WantedManager(config, adapters);
        wanted.loadAll();
        this.alerts = new AlertManager(adapters);
        alerts.loadAll();
        this.duty = new DutyService(config, adapters);
        this.acts = new ActService(this);
        acts.loadAll();
        this.serviceSheets = new ServiceSheetService(this);

        this.badge = new Tesserino(this);
        this.actBook = new ActBook(this);
        this.menus = new ActMenu(this);

        // Detention reschedules timers on load, so it is created after the managers it depends on.
        this.detention = new DetentionManager(this);
        detention.loadAll();

        this.apiProvider = new OpenFdoApiProvider(this);
        getServer().getServicesManager().register(OpenFdoApi.class, apiProvider, this, ServicePriority.Normal);

        registerCommands();
        registerListeners();
        setupIntegrations();

        getLogger().info("[OpenFDO] Enabled. " + config.corps().all().size() + " corps, "
                + config.acts().all().size() + " act type(s), " + agents.all().size() + " member(s), "
                + dossiers.all().size() + " dossier(s).");
    }

    @Override
    public void onDisable() {
        if (detention != null) {
            detention.shutdown();
        }
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenFdoApi.class, apiProvider);
        }
        if (adapters.storage() != null) {
            adapters.storage().flush();
            adapters.storage().close();
        }
        if (adapters.logging() != null) {
            adapters.logging().close();
        }
    }

    /** Re-reads config, language, messages and persisted data. Adapters are intentionally preserved. */
    public void reloadAll() {
        config.reload();
        languageService.reload();
        messages.reload();
        counters.loadAll();
        agents.loadAll();
        dossiers.loadAll();
        evidence.loadAll();
        wanted.loadAll();
        alerts.loadAll();
        acts.loadAll();
        detention.loadAll();
    }

    private void setupAdapters() {
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setRegion(new NoopRegionAdapter());
        adapters.setLogging(createLoggingAdapter(config.settings().loggingAdapter()));
        adapters.setStorage(createStorageAdapter(config.settings().storageAdapter()));
        adapters.storage().init();
    }

    private StorageAdapter createStorageAdapter(String id) {
        if ("memory".equalsIgnoreCase(id)) {
            return new MemoryStorageAdapter();
        }
        File file = new File(getDataFolder(), config.settings().storageFile());
        return new YamlStorageAdapter(file, getLogger());
    }

    private LoggingAdapter createLoggingAdapter(String id) {
        return switch (id == null ? "file" : id.toLowerCase(Locale.ROOT)) {
            case "none" -> new NoopLoggingAdapter();
            case "console" -> new ConsoleLoggingAdapter(getLogger());
            default -> new FileLoggingAdapter(
                    new File(getDataFolder(), config.settings().loggingFile()).toPath(), getLogger());
        };
    }

    /** Picks up world-facing adapters other plugins have registered with the ServicesManager. */
    private void discoverOptionalAdapters() {
        discover(DetentionAdapter.class).ifPresent(adapter -> {
            adapters.setDetention(adapter);
            getLogger().info("[OpenFDO] Detention adapter active: " + adapter.id());
        });
        discover(EconomyAuditAdapter.class).ifPresent(adapters::setEconomyAudit);
        discover(RadioAdapter.class).ifPresent(adapters::setRadio);
        discover(EvidenceSourceAdapter.class).ifPresent(adapters::setEvidenceSource);
        for (RegisteredServiceProvider<ExternalRecordAdapter> registration
                : getServer().getServicesManager().getRegistrations(ExternalRecordAdapter.class)) {
            adapters.addExternalRecord(registration.getProvider());
        }
    }

    private <T> java.util.Optional<T> discover(Class<T> type) {
        RegisteredServiceProvider<T> registration = getServer().getServicesManager().getRegistration(type);
        return registration == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(registration.getProvider());
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

    private void registerCommands() {
        setExecutor("fdo", new FdoCommand(this));
        setExecutor("atto", new AttoCommand(this));
        setExecutor("registro", new RegistroCommand(this));
        setExecutor("detenzione", new DetenzioneCommand(this));
        setExecutor("allerta", new AllertaCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ActBookListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
    }

    private void setExecutor(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("[OpenFDO] Command missing from plugin.yml: " + name);
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

    public FdoConfig config() {
        return config;
    }

    public FdoMessages messages() {
        return messages;
    }

    public AgentManager agents() {
        return agents;
    }

    public DossierManager dossiers() {
        return dossiers;
    }

    public EvidenceManager evidence() {
        return evidence;
    }

    public WantedManager wanted() {
        return wanted;
    }

    public AlertManager alerts() {
        return alerts;
    }

    public DutyService duty() {
        return duty;
    }

    public ActService acts() {
        return acts;
    }

    public ServiceSheetService serviceSheets() {
        return serviceSheets;
    }

    public DetentionManager detention() {
        return detention;
    }

    public Tesserino badge() {
        return badge;
    }

    public ActBook actBook() {
        return actBook;
    }

    public ActMenu menus() {
        return menus;
    }

    public OpenFdoApi api() {
        return apiProvider;
    }
}
