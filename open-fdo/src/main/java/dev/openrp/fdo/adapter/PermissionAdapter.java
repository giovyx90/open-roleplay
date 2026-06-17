package dev.openrp.fdo.adapter;

import org.bukkit.command.CommandSender;

/**
 * Resolves Bukkit-level permission nodes (the command gate). Real authority - which acts a member may
 * produce - comes from their rank capabilities, never from these nodes. The default reflects
 * LuckPerms automatically because it queries the standard Bukkit permission layer.
 */
public interface PermissionAdapter {

    String id();

    boolean has(CommandSender sender, String permission);
}
