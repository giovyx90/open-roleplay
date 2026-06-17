package dev.openrp.crime.adapter.defaults;

import org.bukkit.command.CommandSender;
import dev.openrp.crime.adapter.PermissionAdapter;

/** Default permission adapter: routes through the Bukkit permission API (reflects LuckPerms etc.). */
public final class BukkitPermissionAdapter implements PermissionAdapter {

    @Override
    public String id() {
        return "bukkit";
    }

    @Override
    public boolean has(CommandSender sender, String node) {
        return sender != null && node != null && sender.hasPermission(node);
    }
}
