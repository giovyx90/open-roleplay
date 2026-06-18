package dev.openrp.politics.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import net.kyori.adventure.text.Component;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.Government;
import dev.openrp.politics.core.PoliticsResult;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/** {@code /politica} - charges, government, acts and the public law registry. */
public final class PoliticaCommand extends BasePoliticsCommand {

    private static final List<String> SUBCOMMANDS = List.of("cariche", "carica", "leggi", "legge",
            "archivio", "atti", "atto", "governo", "nomina", "rimuovi", "veto", "vota", "emergenza",
            "elezioni", "successore", "abroga", "admin");

    public PoliticaCommand(OpenPoliticsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "cariche" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "cariche" -> listCharges(sender);
            case "carica" -> chargeDetail(sender, args);
            case "leggi" -> listLaws(sender);
            case "legge" -> lawDetail(sender, args);
            case "archivio" -> listArchive(sender);
            case "atti" -> listActs(sender);
            case "atto" -> signAct(sender, args);
            case "governo" -> governmentStructure(sender);
            case "nomina" -> appoint(sender, args);
            case "rimuovi" -> remove(sender, args);
            case "veto" -> veto(sender, args);
            case "vota" -> collegiateVote(sender, args);
            case "emergenza" -> emergency(sender, args);
            case "elezioni" -> callElection(sender, args);
            case "successore" -> successor(sender, args);
            case "abroga" -> repeal(sender, args);
            case "admin" -> admin(sender, args);
            default -> plugin.messages().warning(sender, "general.unknown_subcommand");
        }
        return true;
    }

    // --- public reads ------------------------------------------------------------------------

    private void listCharges(CommandSender sender) {
        plugin.messages().info(sender, "charge.list_header");
        for (ChargeDef charge : plugin.config().charges().all()) {
            List<ChargeHolder> holders = plugin.charges().activeHoldersOf(charge.id());
            String who = holders.isEmpty()
                    ? plugin.messages().text(sender, "charge.vacant")
                    : holderNames(holders);
            plugin.messages().plain(sender, "charge.list_line",
                    "id", charge.id(), "charge", charge.displayName(),
                    "holders", who, "max", String.valueOf(charge.maxHolders()));
        }
    }

    private void chargeDetail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "charge.help.carica");
            return;
        }
        ChargeDef charge = plugin.config().charges().get(args[1]).orElse(null);
        if (charge == null) {
            plugin.messages().warning(sender, "charge.unknown", "id", args[1]);
            return;
        }
        plugin.messages().info(sender, "charge.detail_header", "charge", charge.displayName());
        plugin.messages().plain(sender, "charge.detail_meta",
                "government", governmentName(charge.governmentId()),
                "authority", String.valueOf(charge.authorityLevel()),
                "mechanism", charge.mechanism().type(),
                "term", charge.hasTerm() ? charge.termDurationDays() + "d" : "-");
        List<String> caps = new ArrayList<>();
        for (PoliticalCapability capability : charge.capabilities()) {
            caps.add(capability.name());
        }
        plugin.messages().plain(sender, "charge.detail_caps", "caps", String.join(", ", caps));
        List<ChargeHolder> holders = plugin.charges().activeHoldersOf(charge.id());
        plugin.messages().plain(sender, "charge.detail_holders",
                "holders", holders.isEmpty() ? plugin.messages().text(sender, "charge.vacant") : holderNames(holders));
    }

    private void listLaws(CommandSender sender) {
        if (!plugin.config().settings().lawsPublicRegistry() && !isAdmin(sender)) {
            plugin.messages().warning(sender, "law.registry_private");
            return;
        }
        List<Law> active = plugin.laws().active(null);
        plugin.messages().info(sender, "law.list_header", "count", String.valueOf(active.size()));
        for (Law law : active) {
            plugin.messages().plain(sender, "law.list_line",
                    "id", law.id(), "title", law.title(),
                    "category", plugin.config().lawCategories().displayName(law.category()));
        }
    }

    private void lawDetail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().info(sender, "law.help.legge");
            return;
        }
        Law law = plugin.laws().get(args[1]).orElse(null);
        if (law == null) {
            plugin.messages().warning(sender, "law.unknown", "id", args[1]);
            return;
        }
        plugin.messages().info(sender, "law.detail_header", "title", law.title());
        plugin.messages().plain(sender, "law.detail_meta",
                "category", plugin.config().lawCategories().displayName(law.category()),
                "status", law.status().name().toLowerCase(Locale.ROOT),
                "government", governmentName(law.governmentId()));
        for (String line : law.body()) {
            sender.sendMessage(Component.text(line));
        }
    }

    private void listArchive(CommandSender sender) {
        List<Law> archived = plugin.laws().archived();
        plugin.messages().info(sender, "law.archive_header", "count", String.valueOf(archived.size()));
        for (Law law : archived) {
            plugin.messages().plain(sender, "law.archive_line",
                    "id", law.id(), "title", law.title(),
                    "status", law.status().name().toLowerCase(Locale.ROOT));
        }
    }

    private void listActs(CommandSender sender) {
        List<PoliticalAct> recent = plugin.acts().recent(15);
        plugin.messages().info(sender, "act.list_header", "count", String.valueOf(recent.size()));
        for (PoliticalAct act : recent) {
            plugin.messages().plain(sender, "act.list_line",
                    "id", act.displayId(), "title", act.title(),
                    "type", typeName(act.typeId()), "status", act.status().name().toLowerCase(Locale.ROOT));
        }
    }

    private void governmentStructure(CommandSender sender) {
        for (Government government : plugin.governments().activeGovernments()) {
            plugin.messages().info(sender, "government.structure_header",
                    "government", government.displayName(), "sigla", government.sigla());
            for (String chargeId : government.chargeIds()) {
                ChargeDef charge = plugin.config().charges().get(chargeId).orElse(null);
                if (charge == null) {
                    continue;
                }
                List<ChargeHolder> holders = plugin.charges().activeHoldersOf(chargeId);
                plugin.messages().plain(sender, "government.structure_line",
                        "charge", charge.displayName(),
                        "holders", holders.isEmpty()
                                ? plugin.messages().text(sender, "charge.vacant") : holderNames(holders));
            }
        }
    }

    // --- holder actions ----------------------------------------------------------------------

    private void signAct(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        // /politica atto nuovo <tipo> <titolo...>  (or)  /politica atto <tipo> <titolo...>
        int offset = args.length >= 2 && args[1].equalsIgnoreCase("nuovo") ? 2 : 1;
        if (args.length < offset + 2) {
            plugin.messages().info(player, "act.help.atto", "types", String.join(", ", plugin.config().actTypes().ids()));
            return;
        }
        String type = args[offset];
        String title = join(args, offset + 1);
        PoliticsResult result = plugin.acts().sign(player.getUniqueId(), isAdmin(player), type, title, List.of(title));
        send(player, result);
        if (result.success() && plugin.config().settings().actPhysicalBook()) {
            result.act().ifPresent(act -> giveBook(player, act));
        }
    }

    private void appoint(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(player, "charge.help.nomina");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(player, "general.player_not_found", "player", args[1]);
            return;
        }
        send(player, plugin.charges().appoint(player.getUniqueId(), isAdmin(player), target.getUniqueId(), args[2]));
    }

    private void remove(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(player, "charge.help.rimuovi");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(player, "general.player_not_found", "player", args[1]);
            return;
        }
        send(player, plugin.charges().remove(player.getUniqueId(), isAdmin(player), target.getUniqueId(), args[2]));
    }

    private void veto(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "act.help.veto");
            return;
        }
        send(player, plugin.acts().veto(player.getUniqueId(), isAdmin(player), args[1]));
    }

    private void collegiateVote(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            plugin.messages().info(player, "act.help.vota");
            return;
        }
        dev.openrp.politics.model.BallotChoice choice =
                dev.openrp.politics.model.BallotChoice.fromString(args[2]).orElse(null);
        if (choice == null) {
            plugin.messages().warning(player, "act.bad_choice");
            return;
        }
        send(player, plugin.acts().castCollegiate(player.getUniqueId(), args[1], choice));
    }

    private void emergency(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "emergency.help");
            return;
        }
        ChargeHolder holder = plugin.charges().activeHoldersOf(player.getUniqueId()).stream()
                .filter(h -> plugin.config().charges().get(h.chargeId())
                        .map(c -> c.grants(PoliticalCapability.DECLARE_EMERGENCY)).orElse(false))
                .findFirst().orElse(null);
        if (holder == null && !isAdmin(player)) {
            plugin.messages().warning(player, "general.no_capability");
            return;
        }
        String governmentId = holder != null ? holder.governmentId()
                : plugin.config().governments().ids().stream().findFirst().orElse("");
        if (args[1].equalsIgnoreCase("dichiara")) {
            plugin.adapters().authority().declareEmergency(governmentId, player.getUniqueId(), join(args, 2));
            plugin.messages().success(player, "emergency.declared", "government", governmentName(governmentId));
        } else if (args[1].equalsIgnoreCase("revoca")) {
            plugin.adapters().authority().revokeEmergency(governmentId, player.getUniqueId());
            plugin.messages().success(player, "emergency.revoked", "government", governmentName(governmentId));
        } else {
            plugin.messages().info(player, "emergency.help");
        }
    }

    private void callElection(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        // /politica elezioni indici <carica>
        if (args.length < 3 || !args[1].equalsIgnoreCase("indici")) {
            plugin.messages().info(player, "election.help.indici");
            return;
        }
        send(player, plugin.elections().call(player.getUniqueId(), isAdmin(player), args[2]));
    }

    private void successor(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "charge.help.successore");
            return;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            plugin.messages().warning(player, "general.player_not_found", "player", args[1]);
            return;
        }
        send(player, plugin.charges().designateSuccessor(player.getUniqueId(), target.getUniqueId()));
    }

    private void repeal(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(player, "law.help.abroga");
            return;
        }
        send(player, plugin.laws().repeal(player.getUniqueId(), isAdmin(player), args[1]));
    }

    // --- admin -------------------------------------------------------------------------------

    private void admin(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            plugin.messages().warning(sender, "general.no_permission");
            return;
        }
        if (args.length < 2) {
            plugin.messages().info(sender, "admin.help");
            return;
        }
        String area = args[1].toLowerCase(Locale.ROOT);
        switch (area) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.messages().success(sender, "general.reload_done");
            }
            case "governo" -> adminGovernment(sender, args);
            case "carica" -> adminCharge(sender, args);
            case "elezione" -> adminElection(sender, args);
            case "legge" -> adminLaw(sender, args);
            default -> plugin.messages().info(sender, "admin.help");
        }
    }

    private void adminGovernment(CommandSender sender, String[] args) {
        // /politica admin governo <attiva|disattiva> <id>
        if (args.length < 4) {
            plugin.messages().info(sender, "admin.help");
            return;
        }
        boolean activate = args[2].equalsIgnoreCase("attiva");
        send(sender, plugin.governments().setActive(args[3], activate));
    }

    private void adminCharge(CommandSender sender, String[] args) {
        // /politica admin carica <assegna <player> <carica>|svuota <carica>>
        if (args.length < 4) {
            plugin.messages().info(sender, "admin.help");
            return;
        }
        if (args[2].equalsIgnoreCase("assegna")) {
            if (args.length < 5) {
                plugin.messages().info(sender, "admin.help");
                return;
            }
            OfflinePlayer target = resolvePlayer(args[3]);
            if (target == null || target.getUniqueId() == null) {
                plugin.messages().warning(sender, "general.player_not_found", "player", args[3]);
                return;
            }
            send(sender, plugin.charges().adminAssign(target.getUniqueId(), args[4]));
        } else if (args[2].equalsIgnoreCase("svuota")) {
            send(sender, plugin.charges().adminVacate(args[3]));
        } else {
            plugin.messages().info(sender, "admin.help");
        }
    }

    private void adminElection(CommandSender sender, String[] args) {
        // /politica admin elezione <chiudi> <id>
        if (args.length < 4) {
            plugin.messages().info(sender, "admin.help");
            return;
        }
        if (args[2].equalsIgnoreCase("chiudi") || args[2].equalsIgnoreCase("annulla")) {
            send(sender, plugin.elections().cancel(args[3]));
        } else {
            plugin.messages().info(sender, "admin.help");
        }
    }

    private void adminLaw(CommandSender sender, String[] args) {
        // /politica admin legge abroga <id>
        if (args.length < 4 || !args[2].equalsIgnoreCase("abroga")) {
            plugin.messages().info(sender, "admin.help");
            return;
        }
        send(sender, plugin.laws().adminRepeal(args[3]));
    }

    // --- helpers -----------------------------------------------------------------------------

    private String holderNames(List<ChargeHolder> holders) {
        List<String> names = new ArrayList<>();
        for (ChargeHolder holder : holders) {
            names.add(playerName(holder.playerUuid()));
        }
        return String.join(", ", names);
    }

    private String playerName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
    }

    private String governmentName(String governmentId) {
        return plugin.config().governments().get(governmentId).map(Government::displayName).orElse(governmentId);
    }

    private String typeName(String typeId) {
        return plugin.config().actTypes().get(typeId).map(t -> t.displayName()).orElse(typeId);
    }

    private void giveBook(Player player, PoliticalAct act) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (book.getItemMeta() instanceof BookMeta meta) {
            meta.title(Component.text(act.title()));
            meta.author(Component.text(playerName(act.authorUuid())));
            List<Component> pages = new ArrayList<>();
            pages.add(Component.text(act.displayId() + "\n\n" + act.title() + "\n\n" + String.join("\n", act.body())));
            meta.pages(pages);
            book.setItemMeta(meta);
        }
        player.getInventory().addItem(book);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestions.filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("carica"))) {
            return CommandSuggestions.filter(plugin.config().charges().ids(), args[1]);
        }
        if (args.length == 2 && sub.equals("atto")) {
            return CommandSuggestions.filter(plugin.config().actTypes().ids(), args[1]);
        }
        if (args.length == 2 && (sub.equals("nomina") || sub.equals("rimuovi") || sub.equals("successore"))) {
            return CommandSuggestions.filter(onlineNames(), args[1]);
        }
        if (args.length == 3 && (sub.equals("nomina") || sub.equals("rimuovi"))) {
            return CommandSuggestions.filter(plugin.config().charges().ids(), args[2]);
        }
        if (args.length == 2 && sub.equals("emergenza")) {
            return CommandSuggestions.filter(List.of("dichiara", "revoca"), args[1]);
        }
        if (args.length == 2 && sub.equals("elezioni")) {
            return CommandSuggestions.filter(List.of("indici"), args[1]);
        }
        if (args.length == 3 && sub.equals("elezioni")) {
            return CommandSuggestions.filter(plugin.config().charges().ids(), args[2]);
        }
        if (args.length == 2 && sub.equals("admin")) {
            return CommandSuggestions.filter(List.of("governo", "carica", "elezione", "legge", "reload"), args[1]);
        }
        return List.of();
    }
}
