package dev.openrp.crime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.adapter.AuthorityAdapter;
import dev.openrp.crime.adapter.CompanyAdapter;
import dev.openrp.crime.adapter.EconomyAdapter;
import dev.openrp.crime.adapter.RegionAdapter;
import dev.openrp.crime.adapter.StorageAdapter;
import dev.openrp.crime.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.crime.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.crime.adapter.defaults.ChunkRegionAdapter;
import dev.openrp.crime.adapter.defaults.InternalEconomyAdapter;
import dev.openrp.crime.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.crime.adapter.defaults.NoopCompanyAdapter;
import dev.openrp.crime.adapter.defaults.PermissionAuthorityAdapter;
import dev.openrp.crime.adapter.defaults.YamlStorageAdapter;
import dev.openrp.crime.api.OpenCrimeApi;
import dev.openrp.crime.api.OpenCrimeApiProvider;
import dev.openrp.crime.command.DenunciaCommand;
import dev.openrp.crime.command.InformatoreCommand;
import dev.openrp.crime.command.OpenCrimeCommand;
import dev.openrp.crime.config.CrimeConfig;
import dev.openrp.crime.core.CrimeEventLog;
import dev.openrp.crime.core.DiscoveryService;
import dev.openrp.crime.core.GoodsService;
import dev.openrp.crime.core.OrgManager;
import dev.openrp.crime.core.TerritoryManager;
import dev.openrp.crime.integration.OpenCoreModuleRegistration;
import dev.openrp.crime.message.CrimeMessages;
import dev.openrp.crime.message.LanguageService;
import dev.openrp.crime.module.CrimeModule;
import dev.openrp.crime.module.laundering.LaunderingModule;
import dev.openrp.crime.module.production.ProductionModule;
import dev.openrp.crime.module.racket.RacketModule;
import dev.openrp.crime.module.syndicate.SyndicateModule;
import dev.openrp.crime.module.traffic.TrafficModule;

/**
 * Open Crime entry point. Wires the config, message layer, adapter set, the core registry services
 * and the toggleable subsystems together. The plugin is fully standalone: OpenCore is optional, and
 * the world-facing adapters (economy, company, authority, region) start as bundled defaults and are
 * replaced at runtime by whatever bridge plugin registers a real one with the ServicesManager.
 * Adapters are created once on enable so integrations swapped in by other plugins survive a reload;
 * only config, messages and data are re-read.
 */
public final class OpenCrimePlugin extends JavaPlugin {

    private final AdapterRegistry adapters = new AdapterRegistry();

    private CrimeConfig config;
    private LanguageService languageService;
    private CrimeMessages messages;
    private InternalEconomyAdapter internalEconomy;

    private GoodsService goods;
    private CrimeEventLog events;
    private DiscoveryService discoveries;
    private OrgManager orgs;
    private TerritoryManager territories;

    private final List<CrimeModule> activeModules = new ArrayList<>();
    private ProductionModule productionModule;
    private TrafficModule trafficModule;
    private LaunderingModule launderingModule;
    private RacketModule racketModule;

    private OpenCrimeApiProvider apiProvider;
    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;

    @Override
    public void onEnable() {
        this.config = new CrimeConfig(this);
        config.reload();
        saveMissingResources();

        this.languageService = new LanguageService(this);
        languageService.reload();
        this.messages = new CrimeMessages(this, languageService);
        messages.reload();

        setupAdapters();

        this.goods = new GoodsService(this, config, adapters);
        goods.loadAll();
        this.events = new CrimeEventLog(adapters);
        events.loadAll();
        this.discoveries = new DiscoveryService(adapters, events);
        discoveries.loadAll();
        this.orgs = new OrgManager(config.hierarchy(), adapters);
        orgs.loadAll();
        this.territories = new TerritoryManager(adapters, orgs);
        territories.loadAll();

        this.apiProvider = new OpenCrimeApiProvider(this);
        getServer().getServicesManager().register(OpenCrimeApi.class, apiProvider, this, ServicePriority.Normal);

        registerCoreCommands();
        enableModules();
        registerDisabledStubs();
        setupIntegrations();

        getLogger().info("[OpenCrime] Enabled. " + config.goods().all().size() + " good(s), "
                + orgs.all().size() + " org(s), modules: " + activeModuleIds() + ".");
    }

    @Override
    public void onDisable() {
        for (CrimeModule module : activeModules) {
            module.disable();
        }
        activeModules.clear();
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenCrimeApi.class, apiProvider);
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
        goods.loadAll();
        events.loadAll();
        discoveries.loadAll();
        orgs.loadAll();
        territories.loadAll();
        if (internalEconomy != null) {
            internalEconomy.load();
        }
        if (productionModule != null) {
            productionModule.service().loadAll();
            productionModule.service().clearNotices();
        }
        if (trafficModule != null) {
            trafficModule.service().loadAll();
        }
        if (launderingModule != null) {
            launderingModule.service().loadAll();
        }
        if (racketModule != null) {
            racketModule.service().loadAll();
        }
    }

    private void setupAdapters() {
        StorageAdapter storage = createStorageAdapter(config.settings().storageAdapter());
        storage.init();
        adapters.setStorage(storage);
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setRegion(new ChunkRegionAdapter());
        this.internalEconomy = new InternalEconomyAdapter(storage);
        internalEconomy.load();
        adapters.setEconomy(internalEconomy);
        adapters.setCompany(new NoopCompanyAdapter());
        adapters.setAuthority(new PermissionAuthorityAdapter());
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
        discover(RegionAdapter.class).ifPresent(adapter -> {
            adapters.setRegion(adapter);
            getLogger().info("[OpenCrime] Region adapter active: " + adapter.id());
        });
        discover(EconomyAdapter.class).ifPresent(adapter -> {
            adapters.setEconomy(adapter);
            getLogger().info("[OpenCrime] Economy adapter active: " + adapter.id());
        });
        discover(CompanyAdapter.class).ifPresent(adapter -> {
            adapters.setCompany(adapter);
            getLogger().info("[OpenCrime] Company adapter active: " + adapter.id());
        });
        discover(AuthorityAdapter.class).ifPresent(adapter -> {
            adapters.setAuthority(adapter);
            getLogger().info("[OpenCrime] Authority adapter active: " + adapter.id());
        });
    }

    private <T> Optional<T> discover(Class<T> type) {
        RegisteredServiceProvider<T> registration = getServer().getServicesManager().getRegistration(type);
        return registration == null ? Optional.empty() : Optional.ofNullable(registration.getProvider());
    }

    private void registerCoreCommands() {
        setExecutor("opencrime", new OpenCrimeCommand(this));
        setExecutor("denuncia", new DenunciaCommand(this));
        setExecutor("informatore", new InformatoreCommand(this));
    }

    private void enableModules() {
        if (config.settings().moduleSyndicate()) {
            register(new SyndicateModule(this));
        }
        if (config.settings().moduleProduction()) {
            this.productionModule = new ProductionModule(this);
            register(productionModule);
        }
        if (config.settings().moduleTraffic()) {
            this.trafficModule = new TrafficModule(this);
            register(trafficModule);
        }
        if (config.settings().moduleLaundering()) {
            this.launderingModule = new LaunderingModule(this);
            register(launderingModule);
        }
        if (config.settings().moduleRacket()) {
            this.racketModule = new RacketModule(this);
            register(racketModule);
        }
    }

    private void register(CrimeModule module) {
        module.enable();
        activeModules.add(module);
    }

    /**
     * Commands of a disabled module still exist in plugin.yml but have no executor; without this Bukkit
     * would print the raw usage string. Give them a stub that says the feature is off.
     */
    private void registerDisabledStubs() {
        if (!config.settings().moduleSyndicate()) {
            stubDisabled("syndicate");
            stubDisabled("territory");
        }
        if (!config.settings().moduleProduction()) {
            stubDisabled("produce");
        }
        if (!config.settings().moduleTraffic()) {
            stubDisabled("traffic");
        }
        if (!config.settings().moduleLaundering()) {
            stubDisabled("launder");
        }
        if (!config.settings().moduleRacket()) {
            stubDisabled("racket");
        }
    }

    private void stubDisabled(String name) {
        setExecutor(name, (sender, command, label, args) -> {
            messages.warning(sender, "general.module_disabled");
            return true;
        });
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
            getLogger().warning("[OpenCrime] Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public String activeModuleIds() {
        if (activeModules.isEmpty()) {
            return "-";
        }
        return activeModules.stream().map(CrimeModule::id).collect(Collectors.joining(", "));
    }

    // --- accessors ---------------------------------------------------------------------------

    public AdapterRegistry adapters() {
        return adapters;
    }

    public CrimeConfig config() {
        return config;
    }

    public CrimeMessages messages() {
        return messages;
    }

    public GoodsService goods() {
        return goods;
    }

    public CrimeEventLog events() {
        return events;
    }

    public DiscoveryService discoveries() {
        return discoveries;
    }

    public OrgManager orgs() {
        return orgs;
    }

    public TerritoryManager territories() {
        return territories;
    }

    public OpenCrimeApi api() {
        return apiProvider;
    }
}
