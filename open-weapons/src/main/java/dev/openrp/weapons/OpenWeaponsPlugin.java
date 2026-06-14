package dev.openrp.weapons;

import dev.openrp.weapons.bridge.OpenCoreBridge;
import dev.openrp.weapons.module.WeaponsModule;
import org.bukkit.plugin.java.JavaPlugin;

public final class OpenWeaponsPlugin extends JavaPlugin {
    private WeaponsModule module;
    private OpenCoreModuleRegistration openCoreRegistration;
    private boolean registeredInOpenCore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.module = new WeaponsModule(this);
        this.openCoreRegistration = new OpenCoreModuleRegistration(this, module);
        if (openCoreRegistration.register()) {
            registeredInOpenCore = true;
        } else {
            try {
                module.onEnable(OpenCoreBridge.unavailable(getLogger()));
                getLogger().info("[OpenWeapons] OpenCore non trovato: plugin avviato in modalita' standalone.");
            } catch (Exception error) {
                getLogger().severe("[OpenWeapons] Avvio standalone fallito: " + rootMessage(error));
                getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    @Override
    public void onDisable() {
        if (registeredInOpenCore && openCoreRegistration != null) {
            openCoreRegistration.unregister();
        } else if (module != null) {
            try {
                module.onDisable();
            } catch (Exception error) {
                getLogger().warning("[OpenWeapons] Arresto modulo fallito: " + rootMessage(error));
            }
        }
        module = null;
        openCoreRegistration = null;
        registeredInOpenCore = false;
    }

    public WeaponsModule getWeaponsModule() {
        return module;
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        if (cause == null) {
            return "Errore sconosciuto";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
