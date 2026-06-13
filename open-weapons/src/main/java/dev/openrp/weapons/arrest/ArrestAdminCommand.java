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
                sender.sendMessage(Component.text("You need openrp.police.arrests.manage to use arrest admin commands.", NamedTextColor.RED));
                return true;
            }
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("You need openrp.police.view to list arrests.", NamedTextColor.RED));
                    return true;
                }
                listArrests(sender);
            }
            case "info" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("You need openrp.police.view to inspect arrests.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /arrests info <player>", NamedTextColor.RED));
                    return true;
                }
                infoArrest(sender, args[1]);
            }
            case "release" -> {
                if (!canManageArrests(sender)) {
                    sender.sendMessage(Component.text("You need openrp.police.arrests.manage to release arrests.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /arrests release <player> [reason]", NamedTextColor.RED));
                    return true;
                }
                String reason = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Released by admin";
                releaseArrest(sender, args[1], reason);
            }
            case "jails" -> {
                if (!canViewArrests(sender)) {
                    sender.sendMessage(Component.text("You need openrp.police.view to list jails.", NamedTextColor.RED));
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
        sender.sendMessage(Component.text("═══ Arrest Admin Commands ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/arrests list", NamedTextColor.YELLOW).append(Component.text(" - List all arrested players", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests info <player>", NamedTextColor.YELLOW).append(Component.text(" - Show arrest details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests release <player> [reason]", NamedTextColor.YELLOW).append(Component.text(" - Force release a player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/arrests jails", NamedTextColor.YELLOW).append(Component.text(" - List all jail regions", NamedTextColor.GRAY)));
    }

    private void listArrests(CommandSender sender) {
        Collection<ArrestRecord> arrests = module.getArrestManager().getAllArrests();
        if (arrests.isEmpty()) {
            sender.sendMessage(Component.text("No players are currently arrested.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("═══ Currently Arrested (" + arrests.size() + ") ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
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
            sender.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return;
        }

        ArrestRecord record = module.getArrestManager().getRecord(target.getUniqueId());
        if (record == null) {
            sender.sendMessage(Component.text(playerName + " is not currently arrested.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══ Arrest Info: " + record.getPlayerName() + " ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Officer: ", NamedTextColor.GRAY).append(Component.text(record.getOfficerName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Reason: ", NamedTextColor.GRAY).append(Component.text(record.getReason(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Jail: ", NamedTextColor.GRAY).append(Component.text(record.getJailRegionId(), NamedTextColor.AQUA)));

        String timeStr;
        if (record.getJailTimeHours() < 1) {
            timeStr = String.format("%.0f minutes", record.getJailTimeHours() * 60);
        } else {
            timeStr = String.format("%.1f hours", record.getJailTimeHours());
        }
        sender.sendMessage(Component.text("  Sentence: ", NamedTextColor.GRAY).append(Component.text(timeStr, NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("  Remaining: ", NamedTextColor.GRAY).append(Component.text(record.getRemainingFormatted(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("  Bail: ", NamedTextColor.GRAY).append(Component.text(record.getBailAmount() > 0 ? "$" + String.format("%.2f", record.getBailAmount()) : "No bail", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("  Arrested at: ", NamedTextColor.GRAY).append(Component.text(DATE_FMT.format(record.getArrestTime()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Release at: ", NamedTextColor.GRAY).append(Component.text(DATE_FMT.format(record.getReleaseTime()), NamedTextColor.WHITE)));
    }

    private void releaseArrest(CommandSender sender, String playerName, String reason) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return;
        }

        if (!module.getArrestManager().isArrested(target.getUniqueId())) {
            sender.sendMessage(Component.text(playerName + " is not currently arrested.", NamedTextColor.RED));
            return;
        }

        module.getArrestManager().release(target.getUniqueId(), reason);
        sender.sendMessage(Component.text("Released " + playerName + " from jail. Reason: " + reason, NamedTextColor.GREEN));
    }

    private void listJails(CommandSender sender) {
        List<String> jails = module.getArrestManager().getJailRegions();
        if (jails.isEmpty()) {
            sender.sendMessage(Component.text("No jail regions found. Create WorldGuard regions starting with 'jail_'.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("═══ Available Jail Regions (" + jails.size() + ") ═══", NamedTextColor.GOLD, TextDecoration.BOLD));
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
