package dev.openrp.core.api;

import dev.openrp.core.api.database.OpenDatabase;
import dev.openrp.core.api.hud.OpenHudService;
import dev.openrp.core.api.module.OpenModuleManager;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.Executor;

public interface OpenRoleplayCore {
    Plugin plugin();

    OpenModuleManager modules();

    Optional<OpenDatabase> database();

    OpenHudService hud();

    Executor asyncExecutor();

    default boolean isDatabaseAvailable() {
        return database().map(OpenDatabase::isReady).orElse(false);
    }
}
