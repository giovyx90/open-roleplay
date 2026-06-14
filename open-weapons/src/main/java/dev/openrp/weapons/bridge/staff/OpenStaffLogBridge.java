package dev.openrp.weapons.bridge.staff;

import org.bukkit.plugin.Plugin;

public final class OpenStaffLogBridge {
    private final Plugin plugin;

    public OpenStaffLogBridge(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void emit(Plugin plugin, StaffBoardLogEvent event) {
        if (plugin != null && event != null) {
            plugin.getLogger().fine("[OpenStaffLog] " + event.type() + " " + event.message());
        }
    }

    public static void emit(StaffBoardLogEvent event) {
        // No global public staff log bus exists yet.
    }
}
