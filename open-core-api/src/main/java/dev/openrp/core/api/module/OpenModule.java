package dev.openrp.core.api.module;

import dev.openrp.core.api.OpenRoleplayCore;

public interface OpenModule {
    String id();

    void onEnable(OpenRoleplayCore core) throws Exception;

    void onDisable() throws Exception;

    default void onReload(OpenRoleplayCore core) throws Exception {
        onDisable();
        onEnable(core);
    }
}
