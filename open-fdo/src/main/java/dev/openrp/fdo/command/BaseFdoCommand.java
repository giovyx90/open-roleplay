package dev.openrp.fdo.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.core.FdoResult;
import dev.openrp.fdo.model.Agent;

/** Shared helpers for the five Open FDO commands: result/permission/agent/capability plumbing. */
abstract class BaseFdoCommand implements CommandExecutor, TabCompleter {

    protected final OpenFdoPlugin plugin;

    protected BaseFdoCommand(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    protected void send(CommandSender sender, FdoResult result) {
        if (result.success()) {
            plugin.messages().success(sender, result.messageKey(), result.placeholders());
        } else {
            plugin.messages().warning(sender, result.messageKey(), result.placeholders());
        }
    }

    protected Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        plugin.messages().warning(sender, "general.player_only");
        return null;
    }

    /** Resolves the sender to an enrolled member, warning and returning {@code null} when they are not. */
    protected Agent requireAgent(Player player) {
        Agent agent = plugin.agents().agent(player.getUniqueId()).orElse(null);
        if (agent == null) {
            plugin.messages().warning(player, "agent.not_enrolled");
        }
        return agent;
    }

    protected boolean isAdmin(CommandSender sender) {
        return plugin.adapters().permission().has(sender, "openfdo.admin");
    }

    /**
     * Verifies the member holds the capability (or is staff). Staff bypass capability checks; everyone
     * else needs the rank capability. Warns and returns {@code false} on failure.
     */
    protected boolean requireCapability(Player player, Agent agent, Capability capability) {
        if (isAdmin(player)) {
            return true;
        }
        if (agent != null && plugin.agents().has(agent.uuid(), capability)) {
            return true;
        }
        plugin.messages().warning(player, "general.no_capability");
        return false;
    }

    protected OfflinePlayer resolvePlayer(String name) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return plugin.getServer().getOfflinePlayerIfCached(name);
    }

    protected static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return fallback;
        }
    }

    protected static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return fallback;
        }
    }

    protected static String join(String[] args, int from) {
        if (from >= args.length) {
            return "";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(args, from, args.length)).trim();
    }
}
