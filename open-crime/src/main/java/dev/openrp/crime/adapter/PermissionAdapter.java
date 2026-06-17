package dev.openrp.crime.adapter;

import org.bukkit.command.CommandSender;

/**
 * Resolves Bukkit/permission-plugin checks. The default reflects whatever permission plugin is
 * installed through the Bukkit permission API; LuckPerms therefore works with no extra code.
 */
public interface PermissionAdapter {

    String id();

    boolean has(CommandSender sender, String node);
}
