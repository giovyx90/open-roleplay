package dev.openrp.vending.adapter.defaults;

import org.bukkit.command.CommandSender;
import dev.openrp.vending.adapter.PermissionAdapter;

/** Default permission adapter: delegates straight to Bukkit permission nodes. */
public final class BukkitPermissionAdapter implements PermissionAdapter {

    @Override
    public String id() {
        return "bukkit";
    }

    @Override
    public boolean has(CommandSender sender, String permission) {
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }
}
