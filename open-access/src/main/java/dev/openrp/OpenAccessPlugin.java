package dev.openrp;

import dev.openrp.access.AccessCommand;
import dev.openrp.access.AccessListener;
import dev.openrp.access.AccessResolver;
import dev.openrp.access.AccessService;
import dev.openrp.access.api.OpenAccessApi;
import dev.openrp.access.gui.AccessGuiListener;
import dev.openrp.access.storage.AccessStorage;
import dev.openrp.access.storage.AccessStorageFactory;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpenAccessPlugin extends JavaPlugin {
    private AccessService service;
    private AccessStorage storage;
    private OpenCoreModuleRegistration openCoreRegistration;
    private final List<Listener> listeners = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("[OpenAccess] WorldGuard non e' caricato: Open Access viene disabilitato.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.storage = AccessStorageFactory.create(this);
        } catch (Exception error) {
            getLogger().severe("[OpenAccess] Impossibile inizializzare lo storage: " + rootMessage(error));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.service = new AccessService(this, storage, new AccessResolver());
        getServer().getServicesManager().register(OpenAccessApi.class, service, this, ServicePriority.Normal);

        AccessCommand command = new AccessCommand(service);
        PluginCommand pluginCommand = getCommand("openaccess");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().warning("[OpenAccess] /openaccess manca in plugin.yml.");
        }

        Set<Material> configuredMaterials = AccessListener.parseConfiguredMaterials(
                getConfig().getStringList("access.additional-interactive-materials"));
        registerListener(new AccessListener(service, configuredMaterials));
        registerListener(new AccessGuiListener());
        this.openCoreRegistration = new OpenCoreModuleRegistration(this, "access");
        this.openCoreRegistration.register();

        service.initialize().whenComplete((ignored, error) -> getServer().getScheduler().runTask(this, () -> {
            if (error != null) {
                getLogger().severe("[OpenAccess] Inizializzazione fallita: " + rootMessage(error));
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("[OpenAccess] Cache caricata con " + service.profiles().size() + " profilo/i.");
        }));
    }

    @Override
    public void onDisable() {
        if (openCoreRegistration != null) {
            openCoreRegistration.unregister();
            openCoreRegistration = null;
        }
        if (service != null) {
            getServer().getServicesManager().unregister(OpenAccessApi.class, service);
        }
        for (Listener listener : listeners) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        if (service != null) {
            service.shutdown();
            service = null;
        }
        if (storage != null) {
            try {
                storage.close();
            } catch (Exception error) {
                getLogger().warning("[OpenAccess] Errore chiudendo lo storage: " + rootMessage(error));
            }
            storage = null;
        }
    }

    public AccessService getAccessService() {
        return service;
    }

    private void registerListener(Listener listener) {
        listeners.add(listener);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        return cause != null && cause.getMessage() != null ? cause.getMessage()
                : cause == null ? "Errore sconosciuto" : cause.getClass().getSimpleName();
    }
}
