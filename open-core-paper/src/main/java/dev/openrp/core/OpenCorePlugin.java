package dev.openrp.core;

import dev.openrp.core.api.OpenRoleplayCore;
import dev.openrp.core.api.database.OpenDatabase;
import dev.openrp.core.api.hud.OpenHudService;
import dev.openrp.core.api.module.OpenModuleManager;
import dev.openrp.core.command.OpenCoreCommand;
import dev.openrp.core.database.OpenDatabaseFactory;
import dev.openrp.core.hud.OpenHudStatusService;
import dev.openrp.core.listener.OpenExperienceControlListener;
import dev.openrp.core.listener.OpenResourcePackSendListener;
import dev.openrp.core.module.OpenPaperModuleManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class OpenCorePlugin extends JavaPlugin implements OpenRoleplayCore {
    private static OpenCorePlugin instance;

    private ExecutorService asyncExecutor;
    private OpenPaperModuleManager moduleManager;
    private OpenHudStatusService hudStatusService;
    private OpenDatabase database;
    private OpenExperienceControlListener experienceControlListener;
    private OpenResourcePackSendListener resourcePackSendListener;
    private CompletableFuture<Optional<OpenDatabase>> databaseInit;

    public static OpenCorePlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.asyncExecutor = Executors.newFixedThreadPool(2, new OpenCoreThreadFactory());
        this.hudStatusService = new OpenHudStatusService();
        this.moduleManager = new OpenPaperModuleManager(this);

        getServer().getServicesManager().register(OpenRoleplayCore.class, this, this, ServicePriority.Normal);
        registerCommand();
        registerOptionalListeners();
        initializeDatabaseThenModules();
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(OpenRoleplayCore.class, this);
        if (databaseInit != null) {
            databaseInit.cancel(false);
            databaseInit = null;
        }
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (experienceControlListener != null) {
            experienceControlListener.stop();
            experienceControlListener = null;
        }
        if (resourcePackSendListener != null) {
            HandlerList.unregisterAll(resourcePackSendListener);
            resourcePackSendListener = null;
        }
        HandlerList.unregisterAll(this);
        if (hudStatusService != null) {
            hudStatusService.clearAll();
        }
        if (database != null) {
            database.close();
            database = null;
        }
        getServer().getScheduler().cancelTasks(this);
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
            asyncExecutor = null;
        }
        instance = null;
    }

    @Override
    public JavaPlugin plugin() {
        return this;
    }

    @Override
    public OpenModuleManager modules() {
        return moduleManager;
    }

    @Override
    public Optional<OpenDatabase> database() {
        return Optional.ofNullable(database).filter(OpenDatabase::isReady);
    }

    @Override
    public OpenHudService hud() {
        return hudStatusService;
    }

    @Override
    public Executor asyncExecutor() {
        return asyncExecutor;
    }

    public OpenPaperModuleManager moduleManager() {
        return moduleManager;
    }

    public void reloadCoreConfig() {
        reloadConfig();
        if (experienceControlListener != null) {
            experienceControlListener.stop();
            HandlerList.unregisterAll(experienceControlListener);
            experienceControlListener = null;
        }
        if (resourcePackSendListener != null) {
            HandlerList.unregisterAll(resourcePackSendListener);
            resourcePackSendListener = null;
        }
        registerOptionalListeners();
    }

    private void registerCommand() {
        OpenCoreCommand command = new OpenCoreCommand(this);
        PluginCommand pluginCommand = getCommand("opencore");
        if (pluginCommand == null) {
            getLogger().warning("[OpenCore] /opencore manca in plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private void registerOptionalListeners() {
        if (getConfig().getBoolean("experience-control.enabled", false)) {
            this.experienceControlListener = new OpenExperienceControlListener(this);
            this.experienceControlListener.start();
        }
        if (getConfig().getBoolean("resource-pack.enabled", false)) {
            this.resourcePackSendListener = new OpenResourcePackSendListener(this);
            getServer().getPluginManager().registerEvents(resourcePackSendListener, this);
        }
    }

    private void initializeDatabaseThenModules() {
        OpenDatabaseFactory.DatabaseSettings databaseSettings = OpenDatabaseFactory.readSettings(getConfig());
        this.databaseInit = CompletableFuture.supplyAsync(() -> {
            if (!databaseSettings.enabled()) {
                getLogger().info("[OpenCore] Database disabilitato da config.yml.");
                return Optional.<OpenDatabase>empty();
            }
            try {
                OpenDatabase created = OpenDatabaseFactory.create(this, databaseSettings);
                getLogger().info("[OpenCore] Database pronto.");
                return Optional.of(created);
            } catch (Exception error) {
                getLogger().warning("[OpenCore] Database non disponibile: " + rootMessage(error));
                return Optional.<OpenDatabase>empty();
            }
        }, asyncExecutor);

        this.databaseInit.whenComplete((created, error) -> {
            Optional<OpenDatabase> createdDatabase = created == null ? Optional.empty() : created;
            if (!isEnabled()) {
                createdDatabase.ifPresent(OpenDatabase::close);
                return;
            }
            try {
                getServer().getScheduler().runTask(this, () -> {
                if (!isEnabled()) {
                    createdDatabase.ifPresent(OpenDatabase::close);
                    return;
                }
                if (error != null) {
                    getLogger().warning("[OpenCore] Inizializzazione database fallita: " + rootMessage(error));
                } else {
                    this.database = createdDatabase.orElse(null);
                }
                moduleManager.loadAll();
                });
            } catch (IllegalStateException schedulerError) {
                createdDatabase.ifPresent(OpenDatabase::close);
            }
        });
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        if (cause == null) {
            return "Errore sconosciuto";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static final class OpenCoreThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "open-core-async-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
