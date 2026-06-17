package dev.openrp.fdo.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.core.FdoResult;
import dev.openrp.fdo.model.Agent;

/** {@code /fdo} - identity and personnel management for an authority member. */
public final class FdoCommand extends BaseFdoCommand {

    private static final List<String> SUBCOMMANDS = List.of("help", "info", "identifica", "tesserino",
            "servizio", "arruola", "congeda", "promuovi", "degrada", "reload");

    public FdoCommand(OpenFdoPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> help(sender);
            case "info" -> info(sender);
            case "identifica" -> identify(sender, args);
            case "tesserino" -> badge(sender);
            case "servizio" -> duty(sender, args);
            case "arruola" -> enroll(sender, args);
            case "congeda" -> discharge(sender, args);
            case "promuovi" -> promote(sender, args, true);
            case "degrada" -> promote(sender, args, false);
            case "reload" -> reload(sender);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void help(CommandSender sender) {
        plugin.messages().info(sender, "fdo.help.header");
        for (String key : List.of("info", "identifica", "tesserino", "servizio")) {
            plugin.messages().info(sender, "fdo.help." + key);
        }
        if (isAdmin(sender) || (sender instanceof Player p && plugin.agents().isApical(p.getUniqueId()))) {
            for (String key : List.of("arruola", "congeda", "promuovi", "degrada")) {
                plugin.messages().info(sender, "fdo.help." + key);
            }
        }
        if (plugin.adapters().permission().has(sender, "openfdo.reload")) {
            plugin.messages().info(sender, "fdo.help.reload");
        }
    }

    private void info(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return;
        }
        String corps = plugin.config().corps().get(agent.corpsId()).map(c -> c.displayName()).orElse(agent.corpsId());
        String rank = plugin.config().ranks().rank(agent.corpsId(), agent.rankId()).map(r -> r.displayName()).orElse(agent.rankId());
        plugin.messages().info(player, "fdo.info_header", "corps", corps);
        plugin.messages().plain(player, "fdo.info_rank", "rank", rank);
        plugin.messages().plain(player, "fdo.info_matricola", "matricola", agent.matricola());
        String caps = plugin.agents().capabilitiesOf(agent.uuid()).stream()
                .map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("-");
        plugin.messages().plain(player, "fdo.info_capabilities", "capabilities", caps);
    }

    private void identify(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return;
        }
        String corps = plugin.config().corps().get(agent.corpsId()).map(c -> c.displayName()).orElse(agent.corpsId());
        String rank = plugin.config().ranks().rank(agent.corpsId(), agent.rankId()).map(r -> r.displayName()).orElse(agent.rankId());
        if (args.length >= 2) {
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
                return;
            }
            plugin.messages().info(target, "fdo.identify_to_target",
                    "name", player.getName(), "corps", corps, "rank", rank, "matricola", agent.matricola());
            plugin.messages().success(sender, "fdo.identify_done", "player", target.getName());
        } else {
            plugin.messages().info(sender, "fdo.identify_self",
                    "corps", corps, "rank", rank, "matricola", agent.matricola());
        }
    }

    private void badge(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return;
        }
        player.getInventory().addItem(plugin.badge().create(agent)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        plugin.messages().success(player, "fdo.badge_issued");
    }

    private void duty(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return;
        }
        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (mode.equals("on")) {
            send(sender, plugin.duty().clockIn(player.getUniqueId()));
        } else if (mode.equals("off")) {
            send(sender, plugin.duty().clockOut(player.getUniqueId()));
        } else {
            plugin.messages().info(sender, "duty.status",
                    "state", plugin.messages().text(sender, plugin.duty().isOnDuty(player.getUniqueId())
                            ? "duty.on" : "duty.off"));
        }
    }

    // --- personnel (admin / apical) ----------------------------------------------------------

    private void enroll(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().info(sender, "fdo.help.arruola");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        String corpsId = args[2];
        if (!canManageCorps(sender, corpsId)) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        String rankId = args.length > 3 ? args[3] : null;
        send(sender, plugin.agents().enroll(target.getUniqueId(),
                target.getName() == null ? args[1] : target.getName(), corpsId, rankId));
    }

    private void discharge(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "fdo.help.congeda");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        Agent agent = target == null ? null : plugin.agents().agent(target.getUniqueId()).orElse(null);
        if (agent == null) {
            plugin.messages().warning(sender, "agent.target_not_enrolled");
            return;
        }
        if (!canManageCorps(sender, agent.corpsId())) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        send(sender, plugin.agents().discharge(agent.uuid()));
    }

    private void promote(CommandSender sender, String[] args, boolean up) {
        if (args.length < 2) {
            plugin.messages().info(sender, "fdo.help." + (up ? "promuovi" : "degrada"));
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        Agent agent = target == null ? null : plugin.agents().agent(target.getUniqueId()).orElse(null);
        if (agent == null) {
            plugin.messages().warning(sender, "agent.target_not_enrolled");
            return;
        }
        if (!canManageCorps(sender, agent.corpsId())) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        FdoResult result = up ? plugin.agents().promote(agent.uuid()) : plugin.agents().demote(agent.uuid());
        send(sender, result);
    }

    private void reload(CommandSender sender) {
        if (!plugin.adapters().permission().has(sender, "openfdo.reload")) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        plugin.reloadAll();
        plugin.messages().success(sender, "general.reload_done");
    }

    /** Staff may manage any corps; an apical member may manage only their own corps. */
    private boolean canManageCorps(CommandSender sender, String corpsId) {
        if (isAdmin(sender)) {
            return true;
        }
        if (sender instanceof Player player) {
            Agent agent = plugin.agents().agent(player.getUniqueId()).orElse(null);
            return agent != null && agent.corpsId().equals(corpsId) && plugin.agents().isApical(agent.uuid());
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("servizio")) {
            return CommandSuggestions.filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && List.of("identifica", "arruola", "congeda", "promuovi", "degrada").contains(sub)) {
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        if (args.length == 3 && sub.equals("arruola")) {
            return CommandSuggestions.filter(plugin.config().corps().ids(), args[2]);
        }
        if (args.length == 4 && sub.equals("arruola")) {
            List<String> ranks = new ArrayList<>();
            plugin.config().ranks().ranks(args[2]).forEach(rank -> ranks.add(rank.id()));
            return CommandSuggestions.filter(ranks, args[3]);
        }
        return List.of();
    }

    private List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(online -> names.add(online.getName()));
        return names;
    }
}
