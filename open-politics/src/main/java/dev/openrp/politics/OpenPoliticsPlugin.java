package dev.openrp.politics;

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
import dev.openrp.politics.adapter.AdapterRegistry;
import dev.openrp.politics.adapter.AuthorityAdapter;
import dev.openrp.politics.adapter.CompanyAdapter;
import dev.openrp.politics.adapter.EconomyAdapter;
import dev.openrp.politics.adapter.IdentityAdapter;
import dev.openrp.politics.adapter.RegionAdapter;
import dev.openrp.politics.adapter.StorageAdapter;
import dev.openrp.politics.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.politics.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.politics.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.politics.adapter.defaults.NoopAuthorityAdapter;
import dev.openrp.politics.adapter.defaults.NoopCompanyAdapter;
import dev.openrp.politics.adapter.defaults.NoopEconomyAdapter;
import dev.openrp.politics.adapter.defaults.NoopIdentityAdapter;
import dev.openrp.politics.adapter.defaults.NoopRegionAdapter;
import dev.openrp.politics.adapter.defaults.YamlStorageAdapter;
import dev.openrp.politics.api.OpenPoliticsApi;
import dev.openrp.politics.api.OpenPoliticsApiProvider;
import dev.openrp.politics.command.OpenPoliticsCommand;
import dev.openrp.politics.command.PoliticaCommand;
import dev.openrp.politics.command.VotoCommand;
import dev.openrp.politics.config.AssignmentMechanism;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.PoliticsConfig;
import dev.openrp.politics.core.ActService;
import dev.openrp.politics.core.ChargeManager;
import dev.openrp.politics.core.ElectionService;
import dev.openrp.politics.core.GovernmentManager;
import dev.openrp.politics.core.LawService;
import dev.openrp.politics.integration.OpenCoreModuleRegistration;
import dev.openrp.politics.message.LanguageService;
import dev.openrp.politics.message.PoliticsMessages;
import dev.openrp.politics.model.ChargeHolder;

/**
 * Open Politics entry point. Wires the config, message layer, adapter set, the manager/service layer
 * and the public API together, and drives one repeating lifecycle task that advances elections, closes
 * collegiate votes and expires mandates. The plugin is fully standalone: OpenCore is optional, and the
 * world-facing adapters (economy, company, identity, region, authority) start as bundled no-ops and are
 * replaced at runtime by whatever bridge plugin registers a real one with the ServicesManager. Adapters
 * are created once on enable so integrations swapped in by other plugins survive a reload; only config,
 * messages and data are re-read.
 */
public final class OpenPoliticsPlugin extends JavaPlugin {

    private final AdapterRegistry adapters = new AdapterRegistry();

    private PoliticsConfig config;
    private LanguageService languageService;
    private PoliticsMessages messages;

    private GovernmentManager governments;
    private ChargeManager charges;
    private ElectionService elections;
    private LawService laws;
    private ActService acts;

    private OpenPoliticsApiProvider apiProvider;
    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;
    private BukkitTask lifecycleTask;

    @Override
    public void onEnable() {
        this.config = new PoliticsConfig(this);
        config.reload();
        saveMissingResources();

        this.languageService = new LanguageService(this);
        languageService.reload();
        this.messages = new PoliticsMessages(this, languageService);
        messages.reload();

        setupAdapters();

        this.governments = new GovernmentManager(config.governments(), adapters);
        governments.loadAll();
        this.charges = new ChargeManager(config, adapters);
        charges.loadAll();
        this.laws = new LawService(this);
        laws.loadAll();
        this.acts = new ActService(this);
        acts.loadAll();
        this.elections = new ElectionService(this);
        elections.loadAll();

        this.apiProvider = new OpenPoliticsApiProvider(this);
        getServer().getServicesManager().register(OpenPoliticsApi.class, apiProvider, this, ServicePriority.Normal);

        registerCommands();
        startLifecycleTask();
        setupIntegrations();

        getLogger().info("[OpenPolitics] Enabled. " + governments.all().size() + " government(s), "
                + config.charges().all().size() + " charge(s), " + laws.active(null).size() + " active law(s).");
    }

    @Override
    public void onDisable() {
        if (lifecycleTask != null) {
            lifecycleTask.cancel();
            lifecycleTask = null;
        }
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenPoliticsApi.class, apiProvider);
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
        governments.loadAll();
        charges.loadAll();
        laws.loadAll();
        acts.loadAll();
        elections.loadAll();
        startLifecycleTask();
    }

    // --- lifecycle ---------------------------------------------------------------------------

    private void startLifecycleTask() {
        if (lifecycleTask != null) {
            lifecycleTask.cancel();
        }
        long period = Math.max(1L, config.settings().electionsCheckIntervalMinutes()) * 60L * 20L;
        this.lifecycleTask = getServer().getScheduler()
                .runTaskTimer(this, this::tickLifecycle, period, period);
    }

    /**
     * One political heartbeat: expire elapsed mandates (re-running an election or applying a succession
     * as the charge's mechanism dictates), advance elections, close due collegiate votes and promulgate
     * acts past their veto window, and reconcile conquest charges with the regions they track.
     */
    private void tickLifecycle() {
        long now = System.currentTimeMillis();
        for (ChargeHolder expired : charges.expireDue(now)) {
            ChargeDef charge = config.charges().get(expired.chargeId()).orElse(null);
            if (charge == null) {
                continue;
            }
            if (charge.mechanism().is(AssignmentMechanism.ELECTION) && config.settings().moduleGovernment()) {
                elections.call(null, true, charge.id());
            } else if (charge.mechanism().is(AssignmentMechanism.HEREDITARY)) {
                charges.applySuccession(expired);
            }
        }
        if (config.settings().moduleGovernment()) {
            elections.tick(now);
            for (ChargeDef charge : config.charges().all()) {
                if (charge.mechanism().is(AssignmentMechanism.CONQUEST)) {
                    charges.reconcileConquest(charge.id());
                }
            }
        }
        if (config.settings().moduleActs()) {
            acts.tick(now);
        }
    }

    // --- adapters ----------------------------------------------------------------------------

    private void setupAdapters() {
        StorageAdapter storage = createStorageAdapter(config.settings().storageAdapter());
        storage.init();
        adapters.setStorage(storage);
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setEconomy(new NoopEconomyAdapter());
        adapters.setCompany(new NoopCompanyAdapter());
        adapters.setIdentity(new NoopIdentityAdapter());
        adapters.setRegion(new NoopRegionAdapter());
        adapters.setAuthority(new NoopAuthorityAdapter());
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
        discover(EconomyAdapter.class).ifPresent(adapter -> {
            adapters.setEconomy(adapter);
            getLogger().info("[OpenPolitics] Economy adapter active: " + adapter.id());
        });
        discover(CompanyAdapter.class).ifPresent(adapter -> {
            adapters.setCompany(adapter);
            getLogger().info("[OpenPolitics] Company adapter active: " + adapter.id());
        });
        discover(IdentityAdapter.class).ifPresent(adapter -> {
            adapters.setIdentity(adapter);
            getLogger().info("[OpenPolitics] Identity adapter active: " + adapter.id());
        });
        discover(RegionAdapter.class).ifPresent(adapter -> {
            adapters.setRegion(adapter);
            getLogger().info("[OpenPolitics] Region adapter active: " + adapter.id());
        });
        discover(AuthorityAdapter.class).ifPresent(adapter -> {
            adapters.setAuthority(adapter);
            getLogger().info("[OpenPolitics] Authority adapter active: " + adapter.id());
        });
    }

    private <T> Optional<T> discover(Class<T> type) {
        RegisteredServiceProvider<T> registration = getServer().getServicesManager().getRegistration(type);
        return registration == null ? Optional.empty() : Optional.ofNullable(registration.getProvider());
    }

    // --- commands ----------------------------------------------------------------------------

    private void registerCommands() {
        setExecutor("openpolitics", new OpenPoliticsCommand(this));
        setExecutor("politica", new PoliticaCommand(this));
        setExecutor("voto", new VotoCommand(this));
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
            getLogger().warning("[OpenPolitics] Command missing from plugin.yml: " + name);
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

    public PoliticsConfig config() {
        return config;
    }

    public PoliticsMessages messages() {
        return messages;
    }

    public GovernmentManager governments() {
        return governments;
    }

    public ChargeManager charges() {
        return charges;
    }

    public ElectionService elections() {
        return elections;
    }

    public LawService laws() {
        return laws;
    }

    public ActService acts() {
        return acts;
    }

    public OpenPoliticsApi api() {
        return apiProvider;
    }
}
