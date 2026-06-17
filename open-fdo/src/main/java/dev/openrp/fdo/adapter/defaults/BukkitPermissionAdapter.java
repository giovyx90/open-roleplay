package dev.openrp.fdo.adapter.defaults;

import org.bukkit.command.CommandSender;
import dev.openrp.fdo.adapter.PermissionAdapter;

/** Default permission adapter: queries the Bukkit permission layer (which also reflects LuckPerms). */
public final class BukkitPermissionAdapter implements PermissionAdapter {

    @Override
    public String id() {
        return "bukkit";
    }

    @Override
    public boolean has(CommandSender sender, String permission) {
        return sender != null && sender.hasPermission(permission);
    }
}
