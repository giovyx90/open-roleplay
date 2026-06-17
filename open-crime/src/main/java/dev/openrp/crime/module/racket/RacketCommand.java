package dev.openrp.crime.module.racket;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Protection;

/** {@code /racket} - impose and manage protection over companies (and owner-side accept/refuse). */
public final class RacketCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("imponi", "lista", "incassa", "escalation",
            "revoca", "accetta", "rifiuta");

    private final RacketService service;

    public RacketCommand(OpenCrimePlugin plugin, RacketService service) {
        super(plugin);
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String sub = args.length == 0 ? "lista" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "imponi" -> impose(player, args);
            case "incassa" -> requireId(player, args, "racket.help.incassa", id -> service.collect(player, id));
            case "escalation" -> requireId(player, args, "racket.help.escalation", id -> service.escalate(player, id));
            case "revoca" -> requireId(player, args, "racket.help.revoca", id -> service.revoke(player, id));
            case "accetta" -> requireId(player, args, "racket.help.accetta", id -> service.respond(player, id, true));
            case "rifiuta" -> requireId(player, args, "racket.help.rifiuta", id -> service.respond(player, id, false));
            case "lista" -> list(player);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void impose(Player player, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(player, "racket.help.imponi");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().warning(player, "general.player_not_found", "player", args[1]);
            return;
        }
        send(player, service.impose(player, target));
    }

    private void list(Player player) {
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        if (!isAdmin(player) && !plugin.orgs().has(player.getUniqueId(), Capability.RACKET_MANAGE)) {
            plugin.messages().warning(player, "general.no_capability");
            return;
        }
        List<Protection> protections = service.ofOrg(org.id());
        plugin.messages().info(player, "racket.list_header", "count", String.valueOf(protections.size()));
        for (Protection protection : protections) {
            plugin.messages().plain(player, "racket.list_line",
                    "id", protection.id(),
                    "company", plugin.adapters().company().companyName(protection.companyId()),
                    "amount", String.valueOf(protection.amount()),
                    "status", protection.status().name().toLowerCase(Locale.ROOT),
                    "coercion", String.valueOf(protection.coercionLevel()));
        }
    }

    private void requireId(Player player, String[] args, String helpKey,
                           java.util.function.Function<String, dev.openrp.crime.core.CrimeResult> action) {
        if (args.length < 2) {
            plugin.messages().info(player, helpKey);
            return;
        }
        send(player, action.apply(args[1]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("imponi")) {
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        return List.of();
    }
}
