package dev.openrp.crime.module.syndicate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.command.BaseCrimeCommand;
import dev.openrp.crime.command.CommandSuggestions;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Territory;

/** {@code /territory} - claim and manage territory for the organisation. */
public final class TerritoryCommand extends BaseCrimeCommand {

    private static final List<String> SUBCOMMANDS = List.of("claim", "stato", "mappa", "abbandona");

    public TerritoryCommand(OpenCrimePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "stato" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "claim" -> claim(sender);
            case "stato", "mappa" -> status(sender);
            case "abbandona" -> abandon(sender);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    private void claim(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Optional<String> region = resolveRegion(player);
        if (region.isEmpty()) {
            return;
        }
        IllegalOrg before = plugin.territories().controller(region.get()).orElse(null);
        CrimeResult result = plugin.territories().claim(player.getUniqueId(), region.get());
        send(sender, result);
        if (result.success() && before != null && !before.id().equals(
                plugin.orgs().byMember(player.getUniqueId()).map(IllegalOrg::id).orElse(""))
                && plugin.config().settings().territoryContestedNotifyMembers()) {
            notifyMembers(before, "territory.contested_notify", "region", region.get());
        }
    }

    private void status(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        IllegalOrg org = requireOrg(player);
        if (org == null) {
            return;
        }
        List<Territory> territories = plugin.territories().ofOrg(org.id());
        plugin.messages().info(player, "territory.list_header",
                "name", org.name(), "count", String.valueOf(territories.size()));
        for (Territory territory : territories) {
            plugin.messages().plain(player, "territory.list_line",
                    "region", territory.regionId(),
                    "contested", plugin.messages().text(player,
                            territory.contested() ? "territory.yes" : "territory.no"));
        }
    }

    private void abandon(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Optional<String> region = resolveRegion(player);
        if (region.isEmpty()) {
            return;
        }
        send(sender, plugin.territories().abandon(player.getUniqueId(), region.get()));
    }

    /** Resolves the region the player stands in, honouring the {@code require_worldguard} gate. */
    private Optional<String> resolveRegion(Player player) {
        if (plugin.config().settings().territoryRequireWorldguard() && !plugin.adapters().region().available()) {
            plugin.messages().warning(player, "territory.requires_worldguard");
            return Optional.empty();
        }
        Optional<String> region = plugin.adapters().region().regionAt(player.getLocation());
        if (region.isEmpty()) {
            plugin.messages().warning(player, "territory.no_region");
        }
        return region;
    }

    private void notifyMembers(IllegalOrg org, String key, Object... placeholders) {
        org.members().forEach(member -> {
            Player online = plugin.getServer().getPlayer(member.uuid());
            if (online != null) {
                plugin.adapters().notification().send(online, plugin.messages().prefixed(online, key, placeholders));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        return List.of();
    }
}
