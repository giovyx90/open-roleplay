package dev.openrp.crime.module.laundering;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.config.LaunderingMethod;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.LaunderingProcess;

/** {@code /launder} - convert dirty money into clean money. */
public final class LaunderCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("avvia", "stato", "storico");

    private final LaunderingService service;

    public LaunderCommand(OpenCrimePlugin plugin, LaunderingService service) {
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
                if (args.length < 3) {
                    plugin.messages().info(sender, "laundering.help.avvia");
                    methods(player);
                } else {
                    send(sender, service.start(player, args[1], parseLong(args[2], -1)));
                }
            }
            case "stato" -> list(player, true);
            case "storico" -> list(player, false);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void methods(Player player) {
        for (LaunderingMethod method : service.availableMethods()) {
            plugin.messages().plain(player, "laundering.method_line",
                    "id", method.id(), "loss", String.valueOf(method.lossPercentage()),
                    "max", String.valueOf(method.maxPerDay()), "hours", String.valueOf(method.durationHours()));
        }
    }

    private void list(Player player, boolean activeOnly) {
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        List<LaunderingProcess> processes = activeOnly ? service.activeOfOrg(org.id()) : service.ofOrg(org.id());
        plugin.messages().info(player, "laundering.status_header", "count", String.valueOf(processes.size()));
        for (LaunderingProcess process : processes) {
            plugin.messages().plain(player, "laundering.status_line",
                    "method", process.methodId(),
                    "dirty", String.valueOf(process.amountDirty()),
                    "status", process.status().name().toLowerCase(Locale.ROOT),
                    "minutes", String.valueOf(service.remainingMinutes(process)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("avvia")) {
            List<String> ids = new ArrayList<>();
            service.availableMethods().forEach(method -> ids.add(method.id()));
            return CommandSuggestions.filter(ids, args[1]);
        }
        return List.of();
    }
}
