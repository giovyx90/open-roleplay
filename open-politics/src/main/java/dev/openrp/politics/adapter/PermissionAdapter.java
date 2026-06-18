package dev.openrp.politics.adapter;

import org.bukkit.command.CommandSender;

/**
 * Resolves staff/permission-node checks. Political authority is <em>not</em> a permission node - it
 * comes from the charge a player holds. This adapter only answers plugin-admin questions
 * ({@code openpolitics.admin}, {@code openpolitics.reload}). The default routes through Bukkit, which
 * reflects LuckPerms when present.
 */
public interface PermissionAdapter {

    String id();

    boolean has(CommandSender sender, String node);
}
