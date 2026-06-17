package dev.openrp.crime.module.traffic;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Shipment;

/** {@code /traffic} - move illegal goods along physical routes. */
public final class TrafficCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("avvia", "consegna", "stato", "accordo", "log");

    private final TrafficService service;

    public TrafficCommand(OpenCrimePlugin plugin, TrafficService service) {
        super(plugin);
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        String sub = args.length == 0 ? "stato" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "avvia" -> {
                if (args.length < 2) {
                    plugin.messages().info(sender, "traffic.help.avvia");
                } else {
                    send(sender, service.start(player, args[1]));
                }
            }
            case "consegna" -> send(sender, service.deliver(player));
            case "accordo" -> {
                if (args.length < 2) {
                    plugin.messages().info(sender, "traffic.help.accordo");
                } else {
                    send(sender, service.agreement(player, args[1]));
                }
            }
            case "stato" -> list(player, true);
            case "log" -> log(player);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void list(Player player, boolean activeOnly) {
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        List<Shipment> shipments = activeOnly ? service.activeOfOrg(org.id()) : service.ofOrg(org.id());
        plugin.messages().info(player, "traffic.status_header", "count", String.valueOf(shipments.size()));
        for (Shipment shipment : shipments) {
            plugin.messages().plain(player, "traffic.status_line",
                    "route", shipment.routeId(),
                    "status", shipment.status().name().toLowerCase(Locale.ROOT),
                    "goods", String.valueOf(shipment.goods().values().stream().mapToInt(Integer::intValue).sum()));
        }
    }

    private void log(Player player) {
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        if (!isAdmin(player) && !plugin.orgs().has(player.getUniqueId(), Capability.TRAFFIC_LOG)) {
            plugin.messages().warning(player, "general.no_capability");
            return;
        }
        list(player, false);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("avvia")) {
            return CommandSuggestions.filter(plugin.config().routes().ids(), args[1]);
        }
        return List.of();
    }
}
