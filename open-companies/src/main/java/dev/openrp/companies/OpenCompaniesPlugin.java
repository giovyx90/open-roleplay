package dev.openrp.companies;

import java.io.File;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.EconomyAdapter;
import dev.openrp.companies.adapter.LoggingAdapter;
import dev.openrp.companies.adapter.StorageAdapter;
import dev.openrp.companies.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.companies.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.companies.adapter.defaults.ConfigEconomyAdapter;
import dev.openrp.companies.adapter.defaults.ConsoleLoggingAdapter;
import dev.openrp.companies.adapter.defaults.FileLoggingAdapter;
import dev.openrp.companies.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.companies.adapter.defaults.NoopIdentityAdapter;
import dev.openrp.companies.adapter.defaults.NoopLoggingAdapter;
import dev.openrp.companies.adapter.defaults.NoopRegionAdapter;
import dev.openrp.companies.adapter.defaults.YamlStorageAdapter;
import dev.openrp.companies.adapter.vault.VaultEconomyAdapter;
import dev.openrp.companies.api.ChamberService;
import dev.openrp.companies.api.CompanyAssetService;
import dev.openrp.companies.api.CompanyService;
import dev.openrp.companies.api.OpenCompaniesApi;
import dev.openrp.companies.api.OpenCompaniesApiProvider;
import dev.openrp.companies.command.OpenCompaniesCommand;
import dev.openrp.companies.config.CompaniesSettings;
import dev.openrp.companies.core.AssetManager;
import dev.openrp.companies.core.CompanyLocks;
import dev.openrp.companies.core.CompanyManager;
import dev.openrp.companies.core.CompanyValidator;
import dev.openrp.companies.core.DefaultChamberService;
import dev.openrp.companies.core.DefaultCompanyAssetService;
import dev.openrp.companies.core.DefaultCompanyService;
import dev.openrp.companies.integration.OpenCoreModuleRegistration;
import dev.openrp.companies.integration.vending.OpenCompaniesBusinessAdapter;
import dev.openrp.companies.message.CompaniesMessages;
import dev.openrp.companies.message.LanguageService;

/**
 * Open Companies entry point. Wires the configuration, message layer, adapter set, the pure rule
 * engine (managers), the Bukkit-facing services and the public API together, then exposes them through
 * accessor methods used across the plugin.
 *
 * <p>Adapters are created once on enable so integrations swapped in by other plugins survive a
 * {@code /company reload}; only config, messages and persisted data are re-read on reload. The plugin
 * is fully standalone: OpenCore, Vault, LuckPerms, WorldGuard and OpenVendingMachines are all optional
 * and integrated defensively.</p>
 */
public final class OpenCompaniesPlugin extends JavaPlugin {

    private final AdapterRegistry adapters = new AdapterRegistry();

    private CompaniesSettings settings;
    private LanguageService languageService;
    private CompaniesMessages messages;
    private CompanyValidator validator;
    private CompanyLocks locks;
    private CompanyManager companyManager;
    private AssetManager assetManager;

    private CompanyService companyService;
    private ChamberService chamberService;
    private CompanyAssetService assetService;
    private OpenCompaniesApiProvider apiProvider;

    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;
    private boolean vendingIntegrationActive;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveMissingResources();

        this.settings = new CompaniesSettings();
        this.settings.load(getConfig());
        this.languageService = new LanguageService(this);
        this.languageService.reload();
        this.messages = new CompaniesMessages(this, languageService);
        this.messages.reload();

        this.validator = new CompanyValidator(settings);
        this.locks = new CompanyLocks();

        setupAdapters();

        this.companyManager = new CompanyManager(settings, validator, adapters);
        this.companyManager.loadAll();
        this.assetManager = new AssetManager(companyManager, adapters);
        this.assetManager.loadAll();

        this.companyService = new DefaultCompanyService(this);
        this.chamberService = new DefaultChamberService(this);
        this.assetService = new DefaultCompanyAssetService(this);

        this.apiProvider = new OpenCompaniesApiProvider(this);
        getServer().getServicesManager().register(OpenCompaniesApi.class, apiProvider, this, ServicePriority.Normal);

        registerCommands();
        setupIntegrations();

        getLogger().info("[OpenCompanies] Enabled. Loaded " + companyManager.all().size() + " company(ies), "
                + assetManager.count() + " asset(s), creation mode " + settings.creationMode().name() + ".");
    }

    @Override
    public void onDisable() {
        if (vendingIntegrationActive && getServer().getPluginManager().getPlugin("OpenVendingMachines") != null) {
            try {
                OpenCompaniesBusinessAdapter.unregister(this);
            } catch (RuntimeException | LinkageError error) {
                getLogger().warning("[OpenCompanies] Failed to detach vending integration: " + error.getMessage());
            }
        }
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenCompaniesApi.class, apiProvider);
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
        reloadConfig();
        settings.load(getConfig());
        languageService.reload();
        messages.reload();
        companyManager.loadAll();
        assetManager.loadAll();
    }

    private void setupAdapters() {
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setRegion(new NoopRegionAdapter());
        adapters.setIdentity(new NoopIdentityAdapter());
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setEconomy(createEconomyAdapter(getConfig().getString("adapters.economy", "default")));
        adapters.setStorage(createStorageAdapter(getConfig().getString("adapters.storage", "yaml")));
        adapters.setLogging(createLoggingAdapter(getConfig().getString("adapters.logging", "file")));
        adapters.storage().init();
    }

    private EconomyAdapter createEconomyAdapter(String id) {
        if ("vault".equalsIgnoreCase(id)) {
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                try {
                    EconomyAdapter vault = new VaultEconomyAdapter(getLogger());
                    getLogger().info("[OpenCompanies] Using Vault economy adapter.");
                    return vault;
                } catch (RuntimeException exception) {
                    getLogger().warning("[OpenCompanies] Vault economy unavailable, falling back to default: "
                            + exception.getMessage());
                }
            } else {
                getLogger().warning("[OpenCompanies] adapters.economy=vault but Vault is not installed; using default economy.");
            }
        }
        return new ConfigEconomyAdapter(settings.demoStartingBalance());
    }

    private StorageAdapter createStorageAdapter(String id) {
        if ("memory".equalsIgnoreCase(id)) {
            return new MemoryStorageAdapter();
        }
        File file = new File(getDataFolder(), getConfig().getString("storage.file", "companies-data.yml"));
        return new YamlStorageAdapter(file, getLogger());
    }

    private LoggingAdapter createLoggingAdapter(String id) {
        return switch (id == null ? "file" : id.toLowerCase(Locale.ROOT)) {
            case "none" -> new NoopLoggingAdapter();
            case "console" -> new ConsoleLoggingAdapter(getLogger());
            default -> new FileLoggingAdapter(
                    new File(getDataFolder(), getConfig().getString("logging.file", "companies-audit.log")).toPath(),
                    getLogger());
        };
    }

    private void setupIntegrations() {
        this.openCoreRegistration = new OpenCoreModuleRegistration(this);
        this.registeredInOpenCore = openCoreRegistration.register();

        if (settings.vendingIntegrationEnabled()
                && getServer().getPluginManager().getPlugin("OpenVendingMachines") != null) {
            try {
                this.vendingIntegrationActive = OpenCompaniesBusinessAdapter.register(this);
                if (vendingIntegrationActive) {
                    getLogger().info("[OpenCompanies] Registered company BusinessAdapter with OpenVendingMachines.");
                }
            } catch (RuntimeException | LinkageError error) {
                getLogger().warning("[OpenCompanies] OpenVendingMachines found, but integration failed: "
                        + error.getMessage());
            }
        }
    }

    private void saveMissingResources() {
        for (String resource : List.of("messages_en.yml", "messages_it.yml")) {
            if (!new File(getDataFolder(), resource).exists()) {
                saveResource(resource, false);
            }
        }
    }

    private void registerCommands() {
        setExecutor("company", new OpenCompaniesCommand(this));
    }

    private void setExecutor(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("[OpenCompanies] Command missing from plugin.yml: " + name);
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

    public CompaniesSettings settings() {
        return settings;
    }

    public LanguageService languageService() {
        return languageService;
    }

    public CompaniesMessages messages() {
        return messages;
    }

    public CompanyValidator validator() {
        return validator;
    }

    public CompanyLocks locks() {
        return locks;
    }

    public CompanyManager companyManager() {
        return companyManager;
    }

    public AssetManager assetManager() {
        return assetManager;
    }

    public CompanyService companies() {
        return companyService;
    }

    public ChamberService chamber() {
        return chamberService;
    }

    public CompanyAssetService assets() {
        return assetService;
    }

    public OpenCompaniesApi api() {
        return apiProvider;
    }
}
