package dev.openrp.politics.adapter.defaults;

import org.bukkit.command.CommandSender;
import dev.openrp.politics.adapter.PermissionAdapter;

/** Default permission adapter: routes admin/reload nodes through Bukkit (reflecting LuckPerms). */
public final class BukkitPermissionAdapter implements PermissionAdapter {

    @Override
    public String id() {
        return "bukkit";
    }

    @Override
    public boolean has(CommandSender sender, String node) {
        return sender != null && sender.hasPermission(node);
    }
}
