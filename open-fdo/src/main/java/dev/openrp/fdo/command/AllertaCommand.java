package dev.openrp.fdo.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.core.FdoResult;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.AlertState;

/** {@code /allerta} - declare, clear or inspect the server-wide alert state. */
public final class AllertaCommand extends BaseFdoCommand {

    private static final List<String> SUBCOMMANDS = List.of("stato", "dichiara", "revoca");

    public AllertaCommand(OpenFdoPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "stato" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "stato" -> status(sender);
            case "dichiara" -> declare(sender, args);
            case "revoca" -> clear(sender);
            default -> status(sender);
        }
        return true;
    }

    private void status(CommandSender sender) {
        AlertState state = plugin.alerts().current();
        if (!state.active()) {
            plugin.messages().info(sender, "alert.status_none");
            return;
        }
        plugin.messages().info(sender, "alert.status_active", "level", state.level(), "reason",
                state.reason().isBlank() ? "-" : state.reason());
    }

    private void declare(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null || !requireCapability(player, agent, Capability.DECLARE_ALERT)) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "alert.help_declare");
            return;
        }
        int level = parseInt(args[1], -1);
        String reason = join(args, 2);
        FdoResult result = plugin.alerts().declare(level, reason, player.getUniqueId());
        send(sender, result);
        if (result.success()) {
            broadcast("alert.broadcast_declared", "level", level, "reason", reason.isBlank() ? "-" : reason);
        }
    }

    private void clear(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null || !requireCapability(player, agent, Capability.DECLARE_ALERT)) {
            return;
        }
        FdoResult result = plugin.alerts().clear();
        send(sender, result);
        if (result.success()) {
            broadcast("alert.broadcast_cleared");
        }
    }

    private void broadcast(String key, Object... placeholders) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            plugin.adapters().notification().notify(online, plugin.messages().prefixed(online, key, placeholders));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        return List.of();
    }
}
