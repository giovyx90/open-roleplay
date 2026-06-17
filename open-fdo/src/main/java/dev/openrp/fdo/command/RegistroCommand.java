package dev.openrp.fdo.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.config.Corps;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.Charge;
import dev.openrp.fdo.model.CustodyEntry;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.WantedEntry;

/** {@code /registro} - consult the archives and run proceeding-desk operations on a dossier. */
public final class RegistroCommand extends BaseFdoCommand {

    private static final List<String> SUBCOMMANDS = List.of("fascicolo", "lista", "capo", "wanted", "servizio", "prova", "help");

    public RegistroCommand(OpenFdoPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        Agent agent = requireAgent(player);
        if (agent == null) {
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "fascicolo" -> dossier(player, args);
            case "lista" -> list(player);
            case "capo" -> charge(player, agent, args);
            case "wanted" -> wanted(player, agent, args);
            case "servizio" -> serviceSheet(player, agent, args);
            case "prova" -> evidence(player, agent, args);
            default -> help(player);
        }
        return true;
    }

    private void help(Player player) {
        plugin.messages().info(player, "registro.help.header");
        for (String key : List.of("fascicolo", "lista", "capo", "wanted", "servizio", "prova")) {
            plugin.messages().info(player, "registro.help." + key);
        }
    }

    private void list(Player player) {
        var all = plugin.dossiers().all();
        if (all.isEmpty()) {
            plugin.messages().info(player, "registro.list_empty");
            return;
        }
        plugin.messages().info(player, "registro.list_header", "count", all.size());
        for (Dossier dossier : all) {
            plugin.messages().plain(player, "registro.list_entry",
                    "id", dossier.id(),
                    "subject", dossier.subjectName().isBlank() ? "-" : dossier.subjectName(),
                    "status", plugin.messages().text(player, dossier.status().messageKey()));
        }
    }

    private void dossier(Player player, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(player, "registro.help.fascicolo");
            return;
        }
        Dossier dossier = plugin.dossiers().find(args[1]).orElse(null);
        if (dossier == null) {
            plugin.messages().warning(player, "dossier.not_found", "id", args[1]);
            return;
        }
        plugin.messages().info(player, "registro.dossier_header", "id", dossier.id());
        plugin.messages().plain(player, "registro.dossier_subject",
                "subject", dossier.subjectName().isBlank() ? "-" : dossier.subjectName());
        plugin.messages().plain(player, "registro.dossier_status",
                "status", plugin.messages().text(player, dossier.status().messageKey()));
        if (dossier.hasActiveCustody()) {
            long remaining = Math.max(0L, (dossier.custodyDeadline() - System.currentTimeMillis()) / 1000L);
            plugin.messages().plain(player, "registro.dossier_custody", "remaining", remaining);
        }
        for (Charge charge : dossier.charges()) {
            String label = plugin.config().crimes().get(charge.crimeId()).map(c -> c.label()).orElse(charge.crimeId());
            plugin.messages().plain(player, "registro.dossier_charge", "crime", label);
        }
        for (Evidence evidence : plugin.evidence().forDossier(dossier.id())) {
            plugin.messages().plain(player, "registro.dossier_evidence",
                    "id", evidence.shortId(), "label", evidence.label(),
                    "state", plugin.messages().text(player, evidence.state().messageKey()));
        }
        for (String note : dossier.notes()) {
            plugin.messages().plain(player, "registro.dossier_note", "note", note);
        }
        dossier.verdict().ifPresent(verdict -> plugin.messages().plain(player, "registro.dossier_verdict",
                "outcome", plugin.messages().text(player, verdict.outcome().messageKey())));
    }

    private void charge(Player player, Agent agent, String[] args) {
        if (!requireCapability(player, agent, Capability.ADD_CHARGE)) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(player, "registro.help.capo");
            return;
        }
        send(player, plugin.dossiers().addCharge(args[1], args[2], player.getUniqueId()));
    }

    private void wanted(Player player, Agent agent, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (action.equals("list")) {
            var active = plugin.wanted().active();
            if (active.isEmpty()) {
                plugin.messages().info(player, "wanted.list_empty");
                return;
            }
            plugin.messages().info(player, "wanted.list_header", "count", active.size());
            for (WantedEntry entry : active) {
                String levelLabel = plugin.config().wanted().get(entry.level()).map(l -> l.label()).orElse("L" + entry.level());
                plugin.messages().plain(player, "wanted.list_entry",
                        "name", entry.subjectName(), "level", levelLabel, "reason", entry.reason().isBlank() ? "-" : entry.reason());
            }
            return;
        }
        if (!requireCapability(player, agent, Capability.FLAG_WANTED)) {
            return;
        }
        if (action.equals("add")) {
            if (args.length < 4) {
                plugin.messages().info(player, "registro.help.wanted");
                return;
            }
            OfflinePlayer target = resolvePlayer(args[2]);
            if (target == null || target.getUniqueId() == null) {
                plugin.messages().warning(player, "general.player_not_found", "player", args[2]);
                return;
            }
            int level = parseInt(args[3], -1);
            String reason = join(args, 4);
            send(player, plugin.wanted().flag(target.getUniqueId(),
                    target.getName() == null ? args[2] : target.getName(), level, reason, player.getUniqueId()));
        } else if (action.equals("remove")) {
            if (args.length < 3) {
                plugin.messages().info(player, "registro.help.wanted");
                return;
            }
            OfflinePlayer target = resolvePlayer(args[2]);
            if (target == null || target.getUniqueId() == null) {
                plugin.messages().warning(player, "general.player_not_found", "player", args[2]);
                return;
            }
            send(player, plugin.wanted().clear(target.getUniqueId()));
        } else {
            plugin.messages().info(player, "registro.help.wanted");
        }
    }

    private void serviceSheet(Player player, Agent agent, String[] args) {
        Agent target = agent;
        if (args.length >= 2) {
            OfflinePlayer other = resolvePlayer(args[1]);
            Agent otherAgent = other == null ? null : plugin.agents().agent(other.getUniqueId()).orElse(null);
            if (otherAgent == null) {
                plugin.messages().warning(player, "agent.target_not_enrolled");
                return;
            }
            if (!otherAgent.uuid().equals(agent.uuid()) && !canReviewSheet(player, agent, otherAgent)) {
                plugin.messages().warning(player, "registro.no_jurisdiction");
                return;
            }
            target = otherAgent;
        }
        var sheet = plugin.serviceSheets().sheetFor(target.uuid(), target.name());
        plugin.messages().info(player, "registro.sheet_header", "name", target.name());
        plugin.messages().plain(player, "registro.sheet_duty", "state",
                plugin.messages().text(player, sheet.onDuty() ? "duty.on" : "duty.off"));
        plugin.messages().plain(player, "registro.sheet_acts", "count", sheet.actCount());
    }

    private void evidence(Player player, Agent agent, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "info";
        if (args.length < 3) {
            plugin.messages().info(player, "registro.help.prova");
            return;
        }
        Evidence evidence = plugin.evidence().findByShortId(args[2]).orElse(null);
        if (evidence == null) {
            plugin.messages().warning(player, "evidence.not_found");
            return;
        }
        Location location = player.getLocation();
        String world = location.getWorld() == null ? "world" : location.getWorld().getName();
        switch (action) {
            case "info" -> {
                plugin.messages().info(player, "registro.evidence_header", "id", evidence.shortId(), "label", evidence.label());
                for (CustodyEntry entry : evidence.chain()) {
                    plugin.messages().plain(player, "registro.evidence_chain",
                            "action", plugin.messages().text(player, entry.action().messageKey()),
                            "where", entry.world() + " " + (int) entry.x() + "," + (int) entry.y() + "," + (int) entry.z());
                }
            }
            case "trasferisci" -> {
                if (!requireCapability(player, agent, Capability.SEIZE_EVIDENCE)) {
                    return;
                }
                if (args.length < 4) {
                    plugin.messages().info(player, "registro.help.prova");
                    return;
                }
                OfflinePlayer target = resolvePlayer(args[3]);
                if (target == null || target.getUniqueId() == null) {
                    plugin.messages().warning(player, "general.player_not_found", "player", args[3]);
                    return;
                }
                send(player, plugin.evidence().transfer(evidence.id(), player.getUniqueId(), target.getUniqueId(),
                        world, location.getX(), location.getY(), location.getZ()));
            }
            case "deposita" -> {
                if (!requireCapability(player, agent, Capability.SEIZE_EVIDENCE)) {
                    return;
                }
                send(player, plugin.evidence().deposit(evidence.id(), player.getUniqueId(),
                        world, location.getX(), location.getY(), location.getZ()));
            }
            default -> plugin.messages().info(player, "registro.help.prova");
        }
    }

    private boolean canReviewSheet(Player player, Agent reviewer, Agent target) {
        if (isAdmin(player)) {
            return true;
        }
        Corps corps = plugin.config().corps().get(reviewer.corpsId()).orElse(null);
        return corps != null && corps.hasJurisdictionOver(target.corpsId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "fascicolo", "capo" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(dossierIds(), args[1]);
                }
                if (args.length == 3 && sub.equals("capo")) {
                    return CommandSuggestions.filter(plugin.config().crimes().ids(), args[2]);
                }
            }
            case "wanted" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(List.of("list", "add", "remove"), args[1]);
                }
                if (args.length == 3) {
                    return CommandSuggestions.filter(onlineNames(), args[2]);
                }
            }
            case "servizio" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(onlineNames(), args[1]);
                }
            }
            case "prova" -> {
                if (args.length == 2) {
                    return CommandSuggestions.filter(List.of("info", "trasferisci", "deposita"), args[1]);
                }
                if (args.length == 4 && args[1].equalsIgnoreCase("trasferisci")) {
                    return CommandSuggestions.filter(onlineNames(), args[3]);
                }
            }
            default -> {
                return List.of();
            }
        }
        return List.of();
    }

    private List<String> dossierIds() {
        List<String> ids = new java.util.ArrayList<>();
        plugin.dossiers().all().forEach(dossier -> ids.add(dossier.id()));
        return ids;
    }

    private List<String> onlineNames() {
        List<String> names = new java.util.ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(online -> names.add(online.getName()));
        return names;
    }
}
