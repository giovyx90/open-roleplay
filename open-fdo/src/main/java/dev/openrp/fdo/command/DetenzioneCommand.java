package dev.openrp.fdo.command;

import java.util.List;
import java.util.Locale;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.core.FdoResult;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.DetentionEndReason;
import dev.openrp.fdo.model.DetentionOrder;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.VerdictOutcome;

/** {@code /detenzione} - detention, sentencing and custody management. */
public final class DetenzioneCommand extends BaseFdoCommand {

    private static final List<String> SUBCOMMANDS = List.of("lista", "info", "rilascia", "condanna", "proroga", "help");

    public DetenzioneCommand(OpenFdoPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "lista" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "lista" -> list(sender);
            case "info" -> info(sender, args);
            case "rilascia" -> release(sender, args);
            case "condanna" -> convict(sender, args);
            case "proroga" -> extend(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        plugin.messages().info(sender, "detenzione.help.header");
        for (String key : List.of("lista", "info", "rilascia", "condanna", "proroga")) {
            plugin.messages().info(sender, "detenzione.help." + key);
        }
    }

    private void list(CommandSender sender) {
        var active = plugin.detention().active();
        if (active.isEmpty()) {
            plugin.messages().info(sender, "detenzione.list_empty");
            return;
        }
        plugin.messages().info(sender, "detenzione.list_header", "count", active.size());
        long now = System.currentTimeMillis();
        for (DetentionOrder order : active) {
            long remaining = Math.max(0L, (order.releaseAt() - now) / 1000L);
            plugin.messages().plain(sender, "detenzione.list_entry",
                    "name", order.inmateName(), "dossier", order.dossierId() == null ? "-" : order.dossierId(),
                    "remaining", remaining);
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "detenzione.help.info");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        DetentionOrder order = target == null ? null : plugin.detention().find(target.getUniqueId()).orElse(null);
        if (order == null) {
            plugin.messages().warning(sender, "detention.not_detained");
            return;
        }
        long remaining = Math.max(0L, (order.releaseAt() - System.currentTimeMillis()) / 1000L);
        plugin.messages().info(sender, "detenzione.info",
                "name", order.inmateName(), "dossier", order.dossierId() == null ? "-" : order.dossierId(),
                "level", order.securityLevel(), "remaining", remaining);
    }

    private void release(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null || !requireCapability(player, agent, Capability.MANAGE_DETENTION)) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "detenzione.help.rilascia");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(sender, "general.player_not_found", "player", args[1]);
            return;
        }
        send(sender, plugin.detention().end(target.getUniqueId(), DetentionEndReason.RELEASED));
    }

    /** {@code /detenzione condanna <fascicolo> <colpevole|assolto|archiviato> [oreSentenza] [motivo]} */
    private void convict(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null || !requireCapability(player, agent, Capability.ISSUE_VERDICT)) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "detenzione.help.condanna");
            return;
        }
        String dossierId = args[1];
        VerdictOutcome outcome = parseOutcome(args[2]);
        if (outcome == null) {
            plugin.messages().warning(sender, "verdict.unknown_outcome");
            return;
        }
        long sentenceHours = outcome.carriesSentence() && args.length > 3 ? parseLong(args[3], 0L) : 0L;
        String note = join(args, outcome.carriesSentence() ? 4 : 3);
        FdoResult result = plugin.dossiers().signVerdict(dossierId, outcome, sentenceHours,
                plugin.config().settings().defaultSecurityLevel(), player.getUniqueId(), note);
        send(sender, result);
        if (result.success() && outcome.carriesSentence()) {
            result.dossier().ifPresent(dossier -> beginDetention(sender, dossier));
        }
    }

    private void beginDetention(CommandSender sender, Dossier dossier) {
        if (dossier.subjectUuid() == null || dossier.verdict().isEmpty()) {
            return;
        }
        var verdict = dossier.verdict().get();
        if (verdict.sentenceSeconds() <= 0L) {
            return;
        }
        DetentionOrder order = new DetentionOrder(dossier.subjectUuid(), dossier.subjectName(), dossier.id(),
                verdict.sentenceSeconds(), verdict.securityLevel(), System.currentTimeMillis());
        send(sender, plugin.detention().begin(order));
    }

    private void extend(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Agent agent = requireAgent(player);
        if (agent == null || !requireCapability(player, agent, Capability.EXTEND_CUSTODY)) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(sender, "detenzione.help.proroga");
            return;
        }
        long hours = parseLong(args[2], -1L);
        if (hours <= 0) {
            plugin.messages().warning(sender, "detenzione.invalid_hours");
            return;
        }
        send(sender, plugin.dossiers().extendCustody(args[1], hours));
    }

    private VerdictOutcome parseOutcome(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "colpevole", "guilty" -> VerdictOutcome.GUILTY;
            case "assolto", "acquitted" -> VerdictOutcome.ACQUITTED;
            case "archiviato", "dismissed" -> VerdictOutcome.DISMISSED;
            default -> VerdictOutcome.fromString(value);
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("info") || sub.equals("rilascia"))) {
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        if (args.length == 2 && (sub.equals("condanna") || sub.equals("proroga"))) {
            return CommandSuggestions.filter(openDossierIds(), args[1]);
        }
        if (args.length == 3 && sub.equals("condanna")) {
            return CommandSuggestions.filter(List.of("colpevole", "assolto", "archiviato"), args[2]);
        }
        return List.of();
    }

    private List<String> openDossierIds() {
        List<String> ids = new java.util.ArrayList<>();
        plugin.dossiers().all().forEach(dossier -> {
            if (!dossier.isClosed()) {
                ids.add(dossier.id());
            }
        });
        return ids;
    }

    private List<String> onlineNames() {
        List<String> names = new java.util.ArrayList<>();
        plugin.getServer().getOnlinePlayers().forEach(online -> names.add(online.getName()));
        return names;
    }
}
