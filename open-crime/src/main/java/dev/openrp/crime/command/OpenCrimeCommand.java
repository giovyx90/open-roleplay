package dev.openrp.crime.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import dev.openrp.crime.OpenCrimePlugin;

/** {@code /opencrime} - administration and status. */
public final class OpenCrimeCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("status", "reload");

    public OpenCrimeCommand(OpenCrimePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "status" -> status(sender);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void status(CommandSender sender) {
        plugin.messages().info(sender, "opencrime.status_header");
        plugin.messages().plain(sender, "opencrime.status_modules", "modules", plugin.activeModuleIds());
        plugin.messages().plain(sender, "opencrime.status_counts",
                "orgs", String.valueOf(plugin.orgs().all().size()),
                "events", String.valueOf(plugin.events().all().size()),
                "discoveries", String.valueOf(plugin.discoveries().all().size()));
        plugin.messages().plain(sender, "opencrime.status_adapters",
                "storage", plugin.adapters().storage().id(),
                "region", plugin.adapters().region().id(),
                "economy", plugin.adapters().economy().id(),
                "company", plugin.adapters().company().id(),
                "authority", plugin.adapters().authority().id());
    }

    private void reload(CommandSender sender) {
        if (!plugin.adapters().permission().has(sender, "opencrime.reload")) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        plugin.reloadAll();
        plugin.messages().success(sender, "general.reload_done");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        return List.of();
    }
}
