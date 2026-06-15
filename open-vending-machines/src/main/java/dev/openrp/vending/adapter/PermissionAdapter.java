package dev.openrp.vending.adapter;

import org.bukkit.command.CommandSender;

/**
 * Indirection over permission checks. Defaults to Bukkit permission nodes, but a server can route
 * checks through LuckPerms contexts, a custom rank system, region rules, etc.
 */
public interface PermissionAdapter {

    String id();

    boolean has(CommandSender sender, String permission);
}
