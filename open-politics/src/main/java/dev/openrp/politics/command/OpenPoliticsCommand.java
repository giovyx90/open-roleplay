package dev.openrp.politics.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import dev.openrp.politics.OpenPoliticsPlugin;

/** {@code /openpolitics} - administration and status. */
public final class OpenPoliticsCommand extends BasePoliticsCommand {

    private static final List<String> SUBCOMMANDS = List.of("status", "reload");

    public OpenPoliticsCommand(OpenPoliticsPlugin plugin) {
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
        plugin.messages().info(sender, "openpolitics.status_header");
        plugin.messages().plain(sender, "openpolitics.status_counts",
                "governments", String.valueOf(plugin.governments().activeGovernments().size()),
                "charges", String.valueOf(plugin.config().charges().all().size()),
                "laws", String.valueOf(plugin.laws().active(null).size()),
                "elections", String.valueOf(plugin.elections().open().size()));
        plugin.messages().plain(sender, "openpolitics.status_adapters",
                "storage", plugin.adapters().storage().id(),
                "economy", plugin.adapters().economy().id(),
                "company", plugin.adapters().company().id(),
                "identity", plugin.adapters().identity().id(),
                "region", plugin.adapters().region().id(),
                "authority", plugin.adapters().authority().id());
    }

    private void reload(CommandSender sender) {
        if (!plugin.adapters().permission().has(sender, "openpolitics.reload")) {
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
