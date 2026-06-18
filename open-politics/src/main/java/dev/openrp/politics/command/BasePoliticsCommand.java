package dev.openrp.politics.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.core.PoliticsResult;

/** Shared helpers for the Open Politics commands: result/permission/player plumbing. */
public abstract class BasePoliticsCommand implements CommandExecutor, TabCompleter {

    protected final OpenPoliticsPlugin plugin;

    protected BasePoliticsCommand(OpenPoliticsPlugin plugin) {
        this.plugin = plugin;
    }

    protected void send(CommandSender sender, PoliticsResult result) {
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
        return plugin.adapters().permission().has(sender, "openpolitics.admin");
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

    protected static String join(String[] args, int from) {
        if (from >= args.length) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
