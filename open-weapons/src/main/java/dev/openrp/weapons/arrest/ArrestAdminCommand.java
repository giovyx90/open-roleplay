package dev.openrp.weapons.arrest;

import it.meridian.core.permissions.NextPermissions;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Admin commands for managing arrests.
 * /arrests list         - List all currently arrested players
 * /arrests info <player> - Show detailed info about an arrest
 * /arrests release <player> [reason] - Force release a player
 * /arrests jails        - List all available jail regions
 */
public class ArrestAdminCommand implements CommandExecutor, TabCompleter {
    private final WeaponsModule module;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public ArrestAdminCommand(WeaponsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!canViewArrests(sender)) {
                sender.sendMessage(Component.text("Ti serve openrp.police.arrests.manage per usare i comandi admin degli arresti.", NamedTextColor.RED));
                return true;
            }
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("Ti serve openrp.police.view per vedere la lista degli arresti.", NamedTextColor.RED));
                    return true;
                }
                listArrests(sender);
            }
            case "info" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("Ti serve openrp.police.view per ispezionare gli arresti.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Uso: /arrests info <giocatore>", NamedTextColor.RED));
                    return true;
                }
                infoArrest(sender, args[1]);
            }
            case "release" -> {
                if (!canManageArrests(sender)) {
                    sender.sendMessage(Component.text("Ti serve openrp.police.arrests.manage per scarcerare giocatori.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Uso: /arrests release <giocatore> [motivo]", NamedTextColor.RED));
                    return true;
                }
                String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Rilasciato da admin";
                releaseArrest(sender, args[1], reason);
            }
            case "jails" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("Ti serve openrp.police.view per vedere le celle.", NamedTextColor.RED));
                    return true;
                }
                listJails(sender);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean canViewArrests(CommandSender sender) {
        return NextPermissions.hasAny(sender,
                NextPermissions.Police.VIEW,
                NextPermissions.Police.ARRESTS_MANAGE,
                NextPermissions.Police.ADMIN,
                "openrp.weapons.arrest.admin");
    }

    private boolean canManageArrests(CommandSender sender) {
        return NextPermissions.hasAny(sender,
                NextPermissions.Police.ARRESTS_MANAGE,
                NextPermissions.Police.ADMIN,
                "openrp.weapons.arrest.admin");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Comandi Admin Arresti ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/arrests list", NamedTextColor.YELLOW).append(Component.text(" - Mostra tutti i giocatori arrestati", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests info <giocatore>", NamedTextColor.YELLOW).append(Component.text(" - Mostra i dettagli dell'arresto", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests release <giocatore> [motivo]", NamedTextColor.YELLOW).append(Component.text(" - Scarcerazione forzata di un giocatore", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests jails", NamedTextColor.YELLOW).append(Component.text(" - Mostra tutte le regioni cella", NamedTextColor.GRAY)));
    }

    private void listArrests(CommandSender sender) {
        Collection<ArrestRecord> arrests = module.getArrestManager().getAllArrests();
        if (arrests.isEmpty()) {
            sender.sendMessage(Component.text("Nessun giocatore e' attualmente arrestato.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("═══ Arrestati attuali (" + arrests.size() + ") ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (ArrestRecord record : arrests) {
            boolean online = Bukkit.getPlayer(record.getPlayerUuid()) != null;
            Component status = online
                    ? Component.text(" ● ", NamedTextColor.GREEN)
                    : Component.text(" ● ", NamedTextColor.RED);

            sender.sendMessage(status
                    .append(Component.text(record.getPlayerName(), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(record.getJailRegionId(), NamedTextColor.AQUA))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(record.getRemainingFormatted(), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(record.getReason(), NamedTextColor.GRAY))
            );
        }
    }

    private void infoArrest(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Giocatore non trovato o non online.", NamedTextColor.RED));
            return;
        }

        ArrestRecord record = module.getArrestManager().getRecord(target.getUniqueId());
        if (record == null) {
            sender.sendMessage(Component.text(playerName + " non e' attualmente arrestato.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══ Info arresto: " + record.getPlayerName() + " ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Agente: ", NamedTextColor.GRAY).append(Component.text(record.getOfficerName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Motivo: ", NamedTextColor.GRAY).append(Component.text(record.getReason(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Cella: ", NamedTextColor.GRAY).append(Component.text(record.getJailRegionId(), NamedTextColor.AQUA)));

        String timeStr;
        if (record.getJailTimeHours() < 1) {
            timeStr = String.format("%.0f minuti", record.getJailTimeHours() * 60);
        } else {
            timeStr = String.format("%.1f ore", record.getJailTimeHours());
        }
        sender.sendMessage(Component.text("  Pena: ", NamedTextColor.GRAY).append(Component.text(timeStr, NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("  Rimanente: ", NamedTextColor.GRAY).append(Component.text(record.getRemainingFormatted(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("  Cauzione: ", NamedTextColor.GRAY).append(Component.text(record.getBailAmount() > 0 ? "$" + String.format("%.2f", record.getBailAmount()) : "Nessuna cauzione", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("  Arrestato il: ", NamedTextColor.GRAY).append(Component.text(DATE_FMT.format(record.getArrestTime()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Scarcerazione il: ", NamedTextColor.GRAY).append(Component.text(DATE_FMT.format(record.getReleaseTime()), NamedTextColor.WHITE)));
    }

    private void releaseArrest(CommandSender sender, String playerName, String reason) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Giocatore non trovato o non online.", NamedTextColor.RED));
            return;
        }

        if (!module.getArrestManager().isArrested(target.getUniqueId())) {
            sender.sendMessage(Component.text(playerName + " non e' attualmente arrestato.", NamedTextColor.RED));
            return;
        }

        module.getArrestManager().release(target.getUniqueId(), reason);
        sender.sendMessage(Component.text("Rilasciato " + playerName + " dal carcere. Motivo: " + reason, NamedTextColor.GREEN));
    }

    private void listJails(CommandSender sender) {
        List<String> jails = module.getArrestManager().getJailRegions();
        if (jails.isEmpty()) {
            sender.sendMessage(Component.text("Nessuna regione cella trovata. Crea regioni WorldGuard che iniziano con 'jail_'.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══ Regioni cella disponibili (" + jails.size() + ") ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (String jail : jails) {
            sender.sendMessage(Component.text("  • " + jail, NamedTextColor.AQUA));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (canViewArrests(sender)) {
                completions.addAll(List.of("list", "info", "jails"));
            }
            if (canManageArrests(sender)) {
                completions.add("release");
            }
            return completions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("release"))
                && (args[0].equalsIgnoreCase("info") ? canViewArrests(sender) : canManageArrests(sender))) {
            List<String> names = new ArrayList<>();
            for (ArrestRecord record : module.getArrestManager().getAllArrests()) {
                names.add(record.getPlayerName());
            }
            return names;
        }
        return List.of();
    }
}
