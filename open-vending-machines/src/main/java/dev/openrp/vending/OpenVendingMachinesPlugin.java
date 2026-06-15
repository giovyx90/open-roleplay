package dev.openrp.vending;

import java.io.File;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import dev.openrp.vending.adapter.AdapterRegistry;
import dev.openrp.vending.adapter.EconomyAdapter;
import dev.openrp.vending.adapter.LoggingAdapter;
import dev.openrp.vending.adapter.StorageAdapter;
import dev.openrp.vending.adapter.defaults.BukkitInventoryAdapter;
import dev.openrp.vending.adapter.defaults.BukkitPermissionAdapter;
import dev.openrp.vending.adapter.defaults.ChatNotificationAdapter;
import dev.openrp.vending.adapter.defaults.ConfigBusinessAdapter;
import dev.openrp.vending.adapter.defaults.ConsoleLoggingAdapter;
import dev.openrp.vending.adapter.defaults.DefaultEconomyAdapter;
import dev.openrp.vending.adapter.defaults.FileLoggingAdapter;
import dev.openrp.vending.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.vending.adapter.defaults.NoopLoggingAdapter;
import dev.openrp.vending.adapter.defaults.YamlStorageAdapter;
import dev.openrp.vending.adapter.vault.VaultEconomyAdapter;
import dev.openrp.vending.api.OpenVendingMachinesApi;
import dev.openrp.vending.api.OpenVendingMachinesApiProvider;
import dev.openrp.vending.command.OpenVendingCommand;
import dev.openrp.vending.config.MachineTypeRegistry;
import dev.openrp.vending.config.ProductRegistry;
import dev.openrp.vending.config.VendingSettings;
import dev.openrp.vending.core.CashService;
import dev.openrp.vending.core.CooldownService;
import dev.openrp.vending.core.MachineLocks;
import dev.openrp.vending.core.PurchaseService;
import dev.openrp.vending.core.RestockService;
import dev.openrp.vending.core.VendingMachineManager;
import dev.openrp.vending.gui.DefaultVendingInterface;
import dev.openrp.vending.gui.VendingInterface;
import dev.openrp.vending.hook.HookExecutor;
import dev.openrp.vending.listener.FurnitureInteractionListener;
import dev.openrp.vending.listener.MachineInteractionListener;
import dev.openrp.vending.message.LanguageService;
import dev.openrp.vending.message.VendingMessages;

/**
 * Open Vending Machines entry point. Wires the configuration, registries, adapter set, hooks, core
 * services and UI together, then exposes them through accessor methods used across the plugin and
 * the public API.
 *
 * <p>Adapters are created once on enable so that integrations swapped in by other plugins survive a
 * {@code /ovm reload}; only config, messages, registries and machine data are re-read on reload.</p>
 */
public final class OpenVendingMachinesPlugin extends JavaPlugin {

    private final AdapterRegistry adapters = new AdapterRegistry();

    private VendingSettings settings;
    private LanguageService languageService;
    private VendingMessages messages;
    private MachineTypeRegistry machineTypes;
    private ProductRegistry products;
    private HookExecutor hooks;
    private MachineLocks locks;
    private CooldownService cooldowns;
    private VendingMachineManager machineManager;
    private PurchaseService purchaseService;
    private RestockService restockService;
    private CashService cashService;
    private VendingInterface userInterface;
    private DefaultVendingInterface defaultInterface;
    private OpenVendingMachinesApiProvider apiProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveMissingResources();

        this.settings = new VendingSettings();
        this.settings.load(getConfig());
        this.languageService = new LanguageService(this);
        this.languageService.reload();
        this.messages = new VendingMessages(this, languageService);
        this.messages.reload();

        this.machineTypes = new MachineTypeRegistry(getLogger());
        this.products = new ProductRegistry(getLogger());
        this.machineTypes.load(new File(getDataFolder(), "machines.yml"));
        this.products.load(new File(getDataFolder(), "products.yml"));

        this.hooks = new HookExecutor(getLogger());
        this.locks = new MachineLocks();
        this.cooldowns = new CooldownService();

        setupAdapters();

        this.machineManager = new VendingMachineManager(this);
        this.machineManager.loadAll();
        this.purchaseService = new PurchaseService(this);
        this.restockService = new RestockService(this);
        this.cashService = new CashService(this);

        this.defaultInterface = new DefaultVendingInterface(this);
        this.userInterface = defaultInterface;

        this.apiProvider = new OpenVendingMachinesApiProvider(this);
        getServer().getServicesManager().register(OpenVendingMachinesApi.class, apiProvider, this, ServicePriority.Normal);

        registerCommands();
        registerListeners();

        getLogger().info("[OpenVendingMachines] Enabled. Loaded " + machineManager.count() + " machine(s), "
                + machineTypes.all().size() + " type(s), " + products.all().size() + " product(s).");
    }

    @Override
    public void onDisable() {
        if (machineManager != null && adapters.storage() != null) {
            machineManager.saveAll();
        }
        if (adapters.storage() != null) {
            adapters.storage().close();
        }
        if (adapters.logging() != null) {
            adapters.logging().close();
        }
        if (apiProvider != null) {
            getServer().getServicesManager().unregister(OpenVendingMachinesApi.class, apiProvider);
        }
    }

    /** Re-reads config, language, registries and machine data. Adapters are intentionally preserved. */
    public void reloadAll() {
        reloadConfig();
        settings.load(getConfig());
        languageService.reload();
        messages.reload();
        machineTypes.load(new File(getDataFolder(), "machines.yml"));
        products.load(new File(getDataFolder(), "products.yml"));
        machineManager.loadAll();
    }

    private void setupAdapters() {
        adapters.setEconomy(createEconomyAdapter(getConfig().getString("adapters.economy", "default")));
        adapters.setInventory(new BukkitInventoryAdapter());
        adapters.setBusiness(new ConfigBusinessAdapter(this));
        adapters.setPermission(new BukkitPermissionAdapter());
        adapters.setStorage(createStorageAdapter(getConfig().getString("adapters.storage", "yaml")));
        adapters.setNotification(new ChatNotificationAdapter());
        adapters.setLogging(createLoggingAdapter(getConfig().getString("adapters.logging", "file")));
        adapters.storage().init();
    }

    private EconomyAdapter createEconomyAdapter(String id) {
        if ("vault".equalsIgnoreCase(id)) {
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                try {
                    EconomyAdapter vault = new VaultEconomyAdapter(getLogger());
                    getLogger().info("[OpenVendingMachines] Using Vault economy adapter.");
                    return vault;
                } catch (RuntimeException exception) {
                    getLogger().warning("[OpenVendingMachines] Vault economy unavailable, falling back to default: "
                            + exception.getMessage());
                }
            } else {
                getLogger().warning("[OpenVendingMachines] adapters.economy=vault but Vault is not installed; using default economy.");
            }
        }
        return new DefaultEconomyAdapter(settings.demoStartingBalance());
    }

    private StorageAdapter createStorageAdapter(String id) {
        if ("memory".equalsIgnoreCase(id)) {
            return new MemoryStorageAdapter();
        }
        File file = new File(getDataFolder(), getConfig().getString("storage.file", "machines-data.yml"));
        return new YamlStorageAdapter(file, getLogger());
    }

    private LoggingAdapter createLoggingAdapter(String id) {
        return switch (id == null ? "file" : id.toLowerCase(Locale.ROOT)) {
            case "none" -> new NoopLoggingAdapter();
            case "console" -> new ConsoleLoggingAdapter(getLogger());
            default -> new FileLoggingAdapter(
                    new File(getDataFolder(), getConfig().getString("logging.file", "transactions.log")).toPath(),
                    getLogger());
        };
    }

    private void saveMissingResources() {
        for (String resource : List.of("machines.yml", "products.yml", "branding.yml", "messages_en.yml", "messages_it.yml")) {
            if (!new File(getDataFolder(), resource).exists()) {
                saveResource(resource, false);
            }
        }
    }

    private void registerCommands() {
        setExecutor("openvending", new OpenVendingCommand(this));
    }

    private void setExecutor(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("[OpenVendingMachines] Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(defaultInterface, this);
        getServer().getPluginManager().registerEvents(new MachineInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnitureInteractionListener(this), this);
    }

    // --- accessors ---------------------------------------------------------------------------

    public VendingSettings settings() {
        return settings;
    }

    public LanguageService languageService() {
        return languageService;
    }

    public VendingMessages messages() {
        return messages;
    }

    public MachineTypeRegistry machineTypes() {
        return machineTypes;
    }

    public ProductRegistry products() {
        return products;
    }

    public AdapterRegistry adapters() {
        return adapters;
    }

    public HookExecutor hooks() {
        return hooks;
    }

    public MachineLocks locks() {
        return locks;
    }

    public CooldownService cooldowns() {
        return cooldowns;
    }

    public VendingMachineManager machines() {
        return machineManager;
    }

    public PurchaseService purchases() {
        return purchaseService;
    }

    public RestockService restocks() {
        return restockService;
    }

    public CashService cash() {
        return cashService;
    }

    public VendingInterface userInterface() {
        return userInterface;
    }

    /** Replace the GUI with a custom implementation. The core only talks to {@link VendingInterface}. */
    public void setUserInterface(VendingInterface userInterface) {
        if (userInterface != null) {
            this.userInterface = userInterface;
        }
    }

    public OpenVendingMachinesApi api() {
        return apiProvider;
    }
}
