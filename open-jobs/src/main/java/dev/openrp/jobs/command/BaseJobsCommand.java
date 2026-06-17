package dev.openrp.jobs.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import dev.openrp.jobs.OpenJobsPlugin;
import dev.openrp.jobs.core.JobResult;

/** Shared helpers for the Open Jobs commands: result/permission/player plumbing. */
public abstract class BaseJobsCommand implements CommandExecutor, TabCompleter {

    protected final OpenJobsPlugin plugin;

    protected BaseJobsCommand(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    protected void send(CommandSender sender, JobResult result) {
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

    protected boolean isAdmin(CommandSender sender) {
        return plugin.adapters().permission().has(sender, "openjobs.admin");
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

    protected List<String> jobIds() {
        List<String> ids = new ArrayList<>();
        plugin.config().jobs().all().forEach(job -> ids.add(job.id()));
        return ids;
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
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command,
                                      String alias, String[] args) {
        return List.of();
    }
}
