package dev.openrp.companies.adapter;

import org.bukkit.command.CommandSender;

/**
 * Indirection over permission checks. Defaults to Bukkit permission nodes (which already reflect
 * LuckPerms-managed nodes), but a server can route checks through a custom rank system, contexts,
 * region rules, etc. by swapping this adapter.
 */
public interface PermissionAdapter {

    String id();

    boolean has(CommandSender sender, String permission);
}
