package dev.openrp.fdo.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.config.ActDefinition;
import dev.openrp.fdo.model.Agent;

/** {@code /atto} - produce official documents (acts) and list the ones you may produce. */
public final class AttoCommand extends BaseFdoCommand {

    private static final List<String> SUBCOMMANDS = List.of("nuovo", "lista", "help");

    public AttoCommand(OpenFdoPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return true;
        }
        String sub = args.length == 0 ? "nuovo" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(player, agent);
            case "help" -> help(player);
            case "nuovo" -> create(player, agent, args);
            default -> create(player, agent, prepend("nuovo", args));
        }
        return true;
    }

    private void help(Player player) {
        plugin.messages().info(player, "atto.help.header");
        plugin.messages().info(player, "atto.help.nuovo");
        plugin.messages().info(player, "atto.help.lista");
    }

    private void list(Player player, Agent agent) {
        List<ActDefinition> available = plugin.acts().available(agent);
        if (available.isEmpty()) {
            plugin.messages().info(player, "act.none_available");
            return;
        }
        plugin.messages().info(player, "atto.list_header", "count", available.size());
        for (ActDefinition act : available) {
            plugin.messages().plain(player, "atto.list_entry", "id", act.id(), "name", act.displayName());
        }
    }

    /** {@code /atto nuovo [tipo] [bersaglio]} - opens the picker, or issues a specific act's book. */
    private void create(Player player, Agent agent, String[] args) {
        if (args.length < 2) {
            plugin.menus().open(player, agent);
            return;
        }
        String actId = args[1].toLowerCase(Locale.ROOT);
        String target = args.length > 2 ? args[2] : null;
        send(player, plugin.acts().beginAct(player, actId, target));
    }

    private static String[] prepend(String first, String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = first;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        Agent agent = plugin.agents().agent(player.getUniqueId()).orElse(null);
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nuovo") && agent != null) {
            List<String> ids = new ArrayList<>();
            for (ActDefinition act : plugin.acts().available(agent)) {
                ids.add(act.id());
            }
            return CommandSuggestions.filter(ids, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("nuovo")) {
            return CommandSuggestions.filter(onlineNames(), args[2]);
        }
        return List.of();
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(online -> names.add(online.getName()));
        return names;
    }
}
