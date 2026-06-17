package dev.openrp.crime.module.production;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.ProductionProcess;

/** {@code /produce} - run production of illegal goods. */
public final class ProduceCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("avvia", "stato", "raccogli", "annulla");

    private final ProductionService service;

    public ProduceCommand(OpenCrimePlugin plugin, ProductionService service) {
        super(plugin);
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "stato" : args[0].toLowerCase(Locale.ROOT);
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        switch (sub) {
            case "avvia" -> {
                if (args.length < 2) {
                    plugin.messages().info(sender, "production.help.avvia");
                } else {
                    send(sender, service.start(player, args[1], args.length >= 3 ? args[2] : null));
                }
            }
            case "raccogli" -> send(sender, service.collect(player));
            case "annulla" -> send(sender, service.cancel(player));
            case "stato" -> status(player);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void status(Player player) {
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        List<ProductionProcess> processes = service.ofOrg(org.id());
        plugin.messages().info(player, "production.status_header", "count", String.valueOf(processes.size()));
        for (ProductionProcess process : processes) {
            plugin.messages().plain(player, "production.status_line",
                    "recipe", process.recipeId(), "stage", process.stageId(),
                    "region", process.locationRegion(),
                    "minutes", String.valueOf(service.remainingMinutes(process)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("avvia")) {
            return CommandSuggestions.filter(plugin.config().production().recipeIds(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("avvia")) {
            List<String> stages = new java.util.ArrayList<>();
            plugin.config().production().recipe(args[1])
                    .ifPresent(recipe -> recipe.stages().forEach(stage -> stages.add(stage.id())));
            return CommandSuggestions.filter(stages, args[2]);
        }
        return List.of();
    }
}
