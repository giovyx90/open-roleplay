package dev.openrp.crime.module.syndicate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.config.OrgRank;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.OrgMember;

/** {@code /syndicate} - found and run an illegal organisation. */
public final class SyndicateCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("fonda", "info", "invita", "accetta",
            "espelli", "promuovi", "degrada", "scioglie", "chat", "treasury");
    private static final double FOUNDING_RADIUS = 12.0;

    public SyndicateCommand(OpenCrimePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "fonda" -> found(sender, args);
            case "info" -> info(sender, args);
            case "invita" -> invite(sender, args);
            case "accetta" -> accept(sender);
            case "espelli" -> expel(sender, args);
            case "promuovi" -> rank(sender, args, true);
            case "degrada" -> rank(sender, args, false);
            case "scioglie" -> dissolve(sender);
            case "chat" -> chat(sender, args);
            case "treasury" -> treasury(sender);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void found(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "syndicate.help.fonda");
            return;
        }
        String name = join(args, 1);
        Map<java.util.UUID, String> crew = new LinkedHashMap<>();
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), FOUNDING_RADIUS)) {
            if (!nearby.equals(player) && plugin.orgs().byMember(nearby.getUniqueId()).isEmpty()) {
                crew.put(nearby.getUniqueId(), nearby.getName());
            }
        }
        send(sender, plugin.orgs().found(player.getUniqueId(), player.getName(), name, "", crew));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Optional<IllegalOrg> target = plugin.orgs().get(args[1]);
            if (target.isEmpty()) {
                plugin.messages().warning(sender, "syndicate.unknown_org", "id", args[1]);
                return;
            }
            IllegalOrg org = target.get();
            plugin.messages().info(sender, "syndicate.info_header", "name", org.name());
            plugin.messages().plain(sender, "syndicate.info_public",
                    "status", org.status().name().toLowerCase(Locale.ROOT),
                    "members", String.valueOf(org.memberCount()));
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        OrgMember member = org.member(player.getUniqueId()).orElse(null);
        String rank = member == null ? "-" : plugin.config().hierarchy().rank(member.roleId())
                .map(OrgRank::displayName).orElse(member.roleId());
        plugin.messages().info(player, "syndicate.info_header", "name", org.name());
        plugin.messages().plain(player, "syndicate.info_self",
                "id", org.id(),
                "status", org.status().name().toLowerCase(Locale.ROOT),
                "rank", rank,
                "members", String.valueOf(org.memberCount()),
                "territories", String.valueOf(org.territories().size()));
    }

    private void invite(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "syndicate.help.invita");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        CrimeResult result = plugin.orgs().invite(player.getUniqueId(), target.getUniqueId(), target.getName());
        send(sender, result);
        if (result.success()) {
            plugin.messages().info(target, "syndicate.invite_received",
                    "org", result.org().map(IllegalOrg::name).orElse(""));
        }
    }

    private void accept(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        send(sender, plugin.orgs().accept(player.getUniqueId(), player.getName()));
    }

    private void expel(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "syndicate.help.espelli");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        send(sender, plugin.orgs().expel(player.getUniqueId(), target.getUniqueId()));
    }

    private void rank(CommandSender sender, String[] args, boolean up) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, up ? "syndicate.help.promuovi" : "syndicate.help.degrada");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        send(sender, plugin.orgs().promote(player.getUniqueId(), target.getUniqueId(), up));
    }

    private void dissolve(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        send(sender, plugin.orgs().dissolve(player.getUniqueId()));
    }

    private void chat(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "syndicate.help.chat");
            return;
        }
        String body = join(args, 1);
        Component line = plugin.messages().mini(player, "syndicate.chat_format",
                "org", org.name(), "player", player.getName(), "message", body);
        for (OrgMember member : org.members()) {
            Player online = plugin.getServer().getPlayer(member.uuid());
            if (online != null) {
                online.sendMessage(line);
            }
        }
    }

    private void treasury(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        if (!isAdmin(player) && !plugin.orgs().has(player.getUniqueId(), Capability.VIEW_TREASURY)) {
            plugin.messages().warning(player, "general.no_capability");
            return;
        }
        long clean = plugin.adapters().economy().balance(org.treasury(), false);
        long dirty = plugin.adapters().economy().balance(org.treasury(), true);
        plugin.messages().info(player, "syndicate.treasury_header", "name", org.name());
        plugin.messages().plain(player, "syndicate.treasury_balances",
                "clean", String.valueOf(clean), "dirty", String.valueOf(dirty));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("invita", "espelli", "promuovi", "degrada").contains(sub)) {
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        return List.of();
    }
}
