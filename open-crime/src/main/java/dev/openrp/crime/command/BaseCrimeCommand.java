package dev.openrp.crime.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.model.IllegalOrg;

/** Shared helpers for the Open Crime commands: result/permission/org/player plumbing. */
public abstract class BaseCrimeCommand implements CommandExecutor, TabCompleter {

    protected final OpenCrimePlugin plugin;

    protected BaseCrimeCommand(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    protected void send(CommandSender sender, CrimeResult result) {
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

    /** Resolves the sender's active organisation, warning and returning {@code null} when they have none. */
    protected IllegalOrg requireOrg(Player player) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null) {
            plugin.messages().warning(player, "syndicate.not_in_org");
        }
        return org;
    }

    protected boolean isAdmin(CommandSender sender) {
        return plugin.adapters().permission().has(sender, "opencrime.admin");
    }

    protected OfflinePlayer resolvePlayer(String name) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return plugin.getServer().getOfflinePlayerIfCached(name);
    }

    protected List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(online -> names.add(online.getName()));
        return names;
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

    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender,
                                      org.bukkit.command.Command command, String alias, String[] args) {
        return List.of();
    }
}
