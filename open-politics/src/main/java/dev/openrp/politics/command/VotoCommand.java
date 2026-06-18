package dev.openrp.politics.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.ElectionStatus;

/** {@code /voto} - candidacies and voting in elections. */
public final class VotoCommand extends BasePoliticsCommand {

    private static final List<String> SUBCOMMANDS = List.of("lista", "candidati", "candidatura", "risultati");

    public VotoCommand(OpenPoliticsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "lista" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(sender);
            case "candidati" -> candidates(sender, args);
            case "candidatura" -> candidacy(sender, args);
            case "risultati" -> results(sender, args);
            default -> vote(sender, args); // /voto <elezione_id> <candidato>
        }
        return true;
    }

    private void list(CommandSender sender) {
        List<Election> open = plugin.elections().open();
        plugin.messages().info(sender, "election.list_header", "count", String.valueOf(open.size()));
        for (Election election : open) {
            plugin.messages().plain(sender, "election.list_line",
                    "id", election.id(), "charge", chargeName(election.chargeId()),
                    "status", election.status().name().toLowerCase(Locale.ROOT),
                    "candidates", String.valueOf(election.candidateCount()));
        }
    }

    private void candidates(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "election.help.candidati");
            return;
        }
        Election election = plugin.elections().get(args[1]).orElse(null);
        if (election == null) {
            plugin.messages().warning(sender, "election.unknown", "id", args[1]);
            return;
        }
        plugin.messages().info(sender, "election.candidates_header", "charge", chargeName(election.chargeId()));
        for (Map.Entry<UUID, String> entry : election.candidacies().entrySet()) {
            plugin.messages().plain(sender, "election.candidate_line", "name", entry.getValue());
        }
    }

    private void candidacy(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "election.help.candidatura");
            return;
        }
        send(player, plugin.elections().candidacy(player.getUniqueId(), player.getName(), args[1]));
    }

    private void vote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "election.help.vota");
            return;
        }
        OfflinePlayer candidate = resolvePlayer(args[1]);
        if (candidate == null || candidate.getUniqueId() == null) {
            plugin.messages().warning(player, "general.player_not_found", "player", args[1]);
            return;
        }
        send(player, plugin.elections().vote(player.getUniqueId(), args[0], candidate.getUniqueId()));
    }

    private void results(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "election.help.risultati");
            return;
        }
        Election election = plugin.elections().get(args[1]).orElse(null);
        if (election == null) {
            plugin.messages().warning(sender, "election.unknown", "id", args[1]);
            return;
        }
        if (election.status() != ElectionStatus.CLOSED) {
            plugin.messages().warning(sender, "election.not_closed");
            return;
        }
        plugin.messages().info(sender, "election.results_header", "charge", chargeName(election.chargeId()));
        Map<UUID, Integer> tally = election.tally();
        for (Map.Entry<UUID, Integer> entry : tally.entrySet()) {
            plugin.messages().plain(sender, "election.results_line",
                    "name", playerName(entry.getKey(), election),
                    "votes", String.valueOf(entry.getValue()));
        }
    }

    private String chargeName(String chargeId) {
        return plugin.config().charges().get(chargeId).map(ChargeDef::displayName).orElse(chargeId);
    }

    private String playerName(UUID uuid, Election election) {
        String fromCandidacy = election.candidacies().get(uuid);
        if (fromCandidacy != null && !fromCandidacy.isBlank()) {
            return fromCandidacy;
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString().substring(0, 8) : name;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(SUBCOMMANDS);
            for (Election election : plugin.elections().open()) {
                options.add(election.id());
            }
            return CommandSuggestions.filter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("candidati") || sub.equals("candidatura") || sub.equals("risultati")) {
                List<String> ids = new ArrayList<>();
                for (Election election : plugin.elections().all()) {
                    ids.add(election.id());
                }
                return CommandSuggestions.filter(ids, args[1]);
            }
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        return List.of();
    }
}
