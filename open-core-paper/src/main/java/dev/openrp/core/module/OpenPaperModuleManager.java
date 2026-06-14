package dev.openrp.core.module;

import dev.openrp.core.OpenCorePlugin;
import dev.openrp.core.api.module.OpenModule;
import dev.openrp.core.api.module.OpenModuleManager;
import dev.openrp.core.api.module.OpenModuleReloadResult;
import dev.openrp.core.api.module.OpenModuleState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class OpenPaperModuleManager implements OpenModuleManager {
    private final OpenCorePlugin plugin;
    private final Map<String, OpenModule> registeredModules = new LinkedHashMap<>();
    private final Map<String, OpenModuleState> moduleStates = new LinkedHashMap<>();
    private final Map<String, String> lastErrors = new LinkedHashMap<>();
    private boolean loadedOnce;

    public OpenPaperModuleManager(OpenCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(OpenModule module) {
        if (module == null || module.id() == null || module.id().isBlank()) {
            throw new IllegalArgumentException("Modulo Open Roleplay non valido.");
        }
        String id = normalize(module.id());
        OpenModule previous = registeredModules.get(id);
        if (previous != null && previous != module && state(id) == OpenModuleState.ENABLED) {
            disableModule(id, previous);
        }
        registeredModules.put(id, module);
        moduleStates.put(id, OpenModuleState.DISCOVERED);
        lastErrors.remove(id);
        if (loadedOnce) {
            if (isEnabledByConfig(id)) {
                enableModule(id, module);
            } else {
                moduleStates.put(id, OpenModuleState.DISABLED_BY_CONFIG);
            }
        }
    }

    @Override
    public void unregister(String id) {
        String key = normalize(id);
        OpenModuleState currentState = state(key);
        OpenModule module = registeredModules.remove(key);
        if (module == null) {
            moduleStates.remove(key);
            lastErrors.remove(key);
            return;
        }
        if (currentState == OpenModuleState.ENABLED) {
            disableModule(key, module);
        }
        moduleStates.remove(key);
        lastErrors.remove(key);
    }

    public void loadAll() {
        int enabled = 0;
        for (Map.Entry<String, OpenModule> entry : registeredModules.entrySet()) {
            String id = entry.getKey();
            if (isEnabledByConfig(id)) {
                if (enableModule(id, entry.getValue())) {
                    enabled++;
                }
            } else {
                if (state(id) == OpenModuleState.ENABLED) {
                    disableModule(id, entry.getValue());
                }
                moduleStates.put(id, OpenModuleState.DISABLED_BY_CONFIG);
                lastErrors.remove(id);
            }
        }
        loadedOnce = true;
        plugin.getLogger().info("[OpenCore] " + enabled + "/" + registeredModules.size() + " modulo/i caricati.");
    }

    public void reloadAll() {
        disableAll();
        loadAll();
    }

    public OpenModuleReloadResult reload(String id) {
        String key = normalize(id);
        OpenModule module = registeredModules.get(key);
        if (module == null) {
            return OpenModuleReloadResult.NOT_FOUND;
        }
        if (!isEnabledByConfig(key)) {
            if (state(key) == OpenModuleState.ENABLED) {
                disableModule(key, module);
            }
            moduleStates.put(key, OpenModuleState.DISABLED_BY_CONFIG);
            lastErrors.remove(key);
            return OpenModuleReloadResult.DISABLED_BY_CONFIG;
        }
        if (state(key) == OpenModuleState.ENABLED && !disableModule(key, module)) {
            return OpenModuleReloadResult.FAILED;
        }
        return enableModule(key, module) ? OpenModuleReloadResult.RELOADED : OpenModuleReloadResult.FAILED;
    }

    public void disableAll() {
        List<Map.Entry<String, OpenModule>> entries = new ArrayList<>(registeredModules.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, OpenModule> entry : entries) {
            if (state(entry.getKey()) == OpenModuleState.ENABLED) {
                disableModule(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Map<String, OpenModule> registeredModules() {
        return Collections.unmodifiableMap(registeredModules);
    }

    @Override
    public OpenModuleState state(String id) {
        return moduleStates.getOrDefault(normalize(id), OpenModuleState.DISCOVERED);
    }

    @Override
    public Optional<String> lastError(String id) {
        return Optional.ofNullable(lastErrors.get(normalize(id)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OpenModule> Optional<T> module(Class<T> type) {
        for (Map.Entry<String, OpenModule> entry : registeredModules.entrySet()) {
            OpenModule module = entry.getValue();
            if (type.isInstance(module) && state(entry.getKey()) == OpenModuleState.ENABLED) {
                return Optional.of((T) module);
            }
        }
        return Optional.empty();
    }

    private boolean enableModule(String id, OpenModule module) {
        if (state(id) == OpenModuleState.ENABLED) {
            return true;
        }
        try {
            module.onEnable(plugin);
            moduleStates.put(id, OpenModuleState.ENABLED);
            lastErrors.remove(id);
            plugin.getLogger().info("[OpenCore] Modulo '" + id + "' abilitato.");
            return true;
        } catch (Throwable error) {
            moduleStates.put(id, OpenModuleState.FAILED);
            lastErrors.put(id, rootMessage(error));
            plugin.getLogger().severe("[OpenCore] Modulo '" + id + "' non abilitato: " + rootMessage(error));
            try {
                module.onDisable();
            } catch (Throwable disableError) {
                plugin.getLogger().severe("[OpenCore] Rollback modulo '" + id + "' fallito: " + rootMessage(disableError));
            }
            return false;
        }
    }

    private boolean disableModule(String id, OpenModule module) {
        try {
            module.onDisable();
            moduleStates.put(id, OpenModuleState.DISABLED);
            lastErrors.remove(id);
            plugin.getLogger().info("[OpenCore] Modulo '" + id + "' disabilitato.");
            return true;
        } catch (Throwable error) {
            moduleStates.put(id, OpenModuleState.FAILED);
            lastErrors.put(id, rootMessage(error));
            plugin.getLogger().severe("[OpenCore] Modulo '" + id + "' non disabilitato pulitamente: " + rootMessage(error));
            return false;
        }
    }

    private boolean isEnabledByConfig(String id) {
        String sectionPath = "modules." + id + ".enabled";
        if (plugin.getConfig().contains(sectionPath)) {
            return plugin.getConfig().getBoolean(sectionPath, true);
        }
        return plugin.getConfig().getBoolean("modules." + id, true);
    }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        if (cause == null) {
            return "Errore sconosciuto";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
