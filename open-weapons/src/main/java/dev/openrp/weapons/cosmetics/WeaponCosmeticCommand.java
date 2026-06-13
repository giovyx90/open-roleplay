package dev.openrp.weapons.cosmetics;

import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WeaponCosmeticCommand implements CommandExecutor, TabCompleter {
    private static final String USE_PERMISSION = "openrp.weapons.cosmetic.use";
    private static final String ADMIN_PERMISSION = "openrp.weapons.cosmetic.admin";

    private final WeaponsModule module;
    private final WeaponCosmeticManager manager;

    public WeaponCosmeticCommand(WeaponsModule module, WeaponCosmeticManager manager) {
        this.module = module;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "token" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(Component.text("You don't have permission to create cosmetic tokens.", NamedTextColor.RED));
                    return true;
                }
                handleToken(sender, args);
            }
            case "station" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(Component.text("You don't have permission to manage cosmetic stations.", NamedTextColor.RED));
                    return true;
                }
                handleStation(sender, args);
            }
            case "gui", "open" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(Component.text("You don't have permission to open the cosmetic bench directly.", NamedTextColor.RED));
                    return true;
                }
                handleGui(sender, args);
            }
            case "editor", "edit" -> {
                if (!canUse(sender)) {
                    sender.sendMessage(Component.text("You don't have permission to use weapon cosmetics.", NamedTextColor.RED));
                    return true;
                }
                handleEditor(sender);
            }
            case "color", "colour" -> {
                if (!canUse(sender)) {
                    sender.sendMessage(Component.text("You don't have permission to use weapon cosmetics.", NamedTextColor.RED));
                    return true;
                }
                handleColor(sender, args);
            }
            case "skin" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(Component.text("You don't have permission to apply weapon skins.", NamedTextColor.RED));
                    return true;
                }
                handleSkin(sender, args);
            }
            case "skins" -> {
                if (!sender.hasPermission(ADMIN_PERMISSION)) {
                    sender.sendMessage(Component.text("You don't have permission to list weapon skins.", NamedTextColor.RED));
                    return true;
                }
                handleSkins(sender, args);
            }
            case "clear" -> {
                if (!canUse(sender)) {
                    sender.sendMessage(Component.text("You don't have permission to use weapon cosmetics.", NamedTextColor.RED));
                    return true;
                }
                handleClear(sender, args);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleToken(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /weaponcosmetic token <led|color> <id> [amount] [player]", NamedTextColor.RED));
            return;
        }
        String type = args[1];
        String id = args[2];
        if (!manager.isValidType(type) || !manager.isValidOption(type, id) || WeaponCosmeticManager.normalize(id).equals(WeaponCosmeticManager.NONE)) {
            sender.sendMessage(Component.text("Unknown cosmetic. Use a valid LED id or #RRGGBB color.", NamedTextColor.RED));
            return;
        }
        int amount = args.length >= 4 ? parseAmount(args[3]) : 1;
        Player target;
        if (args.length >= 5) {
            target = Bukkit.getPlayerExact(args[4]);
            if (target == null) {
                sender.sendMessage(Component.text("Target player is not online.", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("Console usage: /weaponcosmetic token <led|color> <id> <amount> <player>");
            return;
        }
        ItemStack token = manager.createToken(type, id, amount);
        if (token == null) {
            sender.sendMessage(Component.text("Could not create cosmetic token.", NamedTextColor.RED));
            return;
        }
        target.getInventory().addItem(token).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        sender.sendMessage(Component.text("Gave " + token.getAmount() + " cosmetic token(s) to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can clear weapon cosmetics.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /weaponcosmetic clear <led|color|skin|all>", NamedTextColor.RED));
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!manager.clearCosmetic(weapon, args[1])) {
            player.sendMessage(Component.text("Hold a supported weapon and choose led, color, skin, or all.", NamedTextColor.RED));
            return;
        }
        module.refreshWeaponVisual(weapon);
        player.getInventory().setItemInMainHand(weapon);
        player.sendMessage(Component.text("Weapon cosmetic cleared.", NamedTextColor.GREEN));
    }

    private void handleStation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weaponcosmetic station <create|remove|list> [id]", NamedTextColor.RED));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can create cosmetic stations.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponcosmetic station create <id>", NamedTextColor.RED));
                    return;
                }
                if (!module.getWeaponCosmeticStationManager().createStation(player, args[2])) {
                    sender.sendMessage(Component.text("Invalid station id.", NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text("Cosmetic station created: "
                        + WeaponCosmeticStationManager.normalizeStationId(args[2]), NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponcosmetic station remove <id>", NamedTextColor.RED));
                    return;
                }
                if (!module.getWeaponCosmeticStationManager().removeStation(args[2])) {
                    sender.sendMessage(Component.text("Unknown station id.", NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text("Cosmetic station removed: "
                        + WeaponCosmeticStationManager.normalizeStationId(args[2]), NamedTextColor.GREEN));
            }
            case "list" -> {
                String stations = module.getWeaponCosmeticStationManager().stations().stream()
                        .map(WeaponCosmeticStationManager.StationRecord::id)
                        .collect(Collectors.joining(", "));
                sender.sendMessage(Component.text(stations.isBlank()
                        ? "No cosmetic stations configured."
                        : "Cosmetic stations: " + stations, NamedTextColor.YELLOW));
            }
            default -> sender.sendMessage(Component.text("Usage: /weaponcosmetic station <create|remove|list> [id]", NamedTextColor.RED));
        }
    }

    private void handleEditor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the weapon cosmetic editor.");
            return;
        }
        module.openWeaponCosmeticEditor(player);
    }

    private void handleColor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can apply weapon colors.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /weaponcosmetic color <#RRGGBB|none>", NamedTextColor.RED));
            return;
        }
        Integer rgb = WeaponCosmeticManager.normalize(args[1]).equals(WeaponCosmeticManager.NONE)
                ? null
                : WeaponCosmeticManager.parseColorRgb(args[1]);
        if (rgb == null && !WeaponCosmeticManager.normalize(args[1]).equals(WeaponCosmeticManager.NONE)) {
            player.sendMessage(Component.text("Use a valid #RRGGBB color or none.", NamedTextColor.RED));
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        WeaponCosmeticManager.CosmeticSelection current = manager.getSelection(weapon);
        if (!manager.applySelection(weapon, WeaponCosmeticManager.NONE, current.ledId(), rgb)) {
            player.sendMessage(Component.text("Hold a supported colorable weapon.", NamedTextColor.RED));
            return;
        }
        module.refreshWeaponVisual(weapon);
        player.getInventory().setItemInMainHand(weapon);
        player.sendMessage(Component.text(rgb == null
                ? "Weapon color cleared."
                : "Weapon color applied: " + WeaponCosmeticManager.formatHex(rgb), NamedTextColor.GREEN));
    }

    private void handleSkin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weaponcosmetic skin <skin-id|none> [player]", NamedTextColor.RED));
            return;
        }
        Player target = resolveTarget(sender, args.length >= 3 ? args[2] : null,
                "Console usage: /weaponcosmetic skin <skin-id|none> <player>");
        if (target == null) {
            return;
        }
        ItemStack weapon = target.getInventory().getItemInMainHand();
        String weaponId = manager.getWeaponId(weapon);
        if (!manager.applySkin(weapon, args[1])) {
            String validSkins = weaponId == null ? "" : String.join(", ", manager.getSkinIds(weaponId));
            target.sendMessage(Component.text(validSkins.isBlank()
                    ? "Hold a skinnable weapon and choose a valid skin id."
                    : "Valid skins for " + weaponId + ": " + validSkins + ", none", NamedTextColor.RED));
            if (!sender.equals(target)) {
                sender.sendMessage(Component.text("Could not apply that skin to " + target.getName() + "'s held weapon.", NamedTextColor.RED));
            }
            return;
        }
        module.refreshWeaponVisual(weapon);
        target.getInventory().setItemInMainHand(weapon);
        String normalizedSkin = WeaponCosmeticManager.normalize(args[1]);
        Component message = Component.text(normalizedSkin.equals(WeaponCosmeticManager.NONE)
                ? "Weapon skin cleared."
                : "Weapon skin applied: " + normalizedSkin, NamedTextColor.GREEN);
        target.sendMessage(message);
        if (!sender.equals(target)) {
            sender.sendMessage(Component.text("Updated held weapon skin for " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleSkins(CommandSender sender, String[] args) {
        String weaponId;
        if (args.length >= 2) {
            weaponId = WeaponCosmeticManager.normalizeWeaponId(args[1]);
        } else if (sender instanceof Player player) {
            weaponId = manager.getWeaponId(player.getInventory().getItemInMainHand());
        } else {
            sender.sendMessage("Console usage: /weaponcosmetic skins <weapon-id>");
            return;
        }
        if (weaponId == null || weaponId.equals(WeaponCosmeticManager.NONE)) {
            sender.sendMessage(Component.text("Hold a skinnable weapon or specify a weapon id.", NamedTextColor.RED));
            return;
        }
        List<String> skinIds = manager.getSkinIds(weaponId);
        sender.sendMessage(Component.text(skinIds.isEmpty()
                ? "No skins registered for " + weaponId + "."
                : "Skins for " + weaponId + ": " + String.join(", ", skinIds), NamedTextColor.YELLOW));
    }

    private Player resolveTarget(CommandSender sender, String targetName, String consoleUsage) {
        if (targetName != null && !targetName.isBlank()) {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(Component.text("Target player is not online.", NamedTextColor.RED));
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(consoleUsage);
        return null;
    }

    private void handleGui(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Target player is not online.", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("Console usage: /weaponcosmetic gui <player>");
            return;
        }

        module.openWeaponCosmeticWorkbench(target);
        if (!sender.equals(target)) {
            sender.sendMessage(Component.text("Opened cosmetic bench for " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private int parseAmount(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void sendUsage(CommandSender sender) {
        List<String> actions = new ArrayList<>();
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            actions.add("token");
            actions.add("station");
            actions.add("gui");
            actions.add("skin");
            actions.add("skins");
        }
        if (canUse(sender)) {
            actions.add("editor");
            actions.add("color");
            actions.add("clear");
        }
        if (actions.isEmpty()) {
            sender.sendMessage(Component.text("You don't have permission to use weapon cosmetics.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Usage: /weaponcosmetic <" + String.join("|", actions) + ">", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean canUse = canUse(sender);
        boolean canAdmin = sender.hasPermission(ADMIN_PERMISSION);
        if (!canUse && !canAdmin) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> actions = new ArrayList<>();
            if (canAdmin) {
                actions.add("token");
                actions.add("station");
                actions.add("gui");
                actions.add("skin");
                actions.add("skins");
            }
            if (canUse) {
                actions.add("editor");
                actions.add("color");
                actions.add("clear");
            }
            return filter(actions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("token") && canAdmin) {
            return filter(List.of("led", "color"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("token") && canAdmin && manager.isValidType(args[1])) {
            List<String> values = new ArrayList<>(manager.getOptionIds(args[1]));
            if (args[1].equalsIgnoreCase(WeaponCosmeticManager.TYPE_COLOR)) {
                values.add("#A14DFF");
            }
            return filter(values, args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("clear") && canUse) {
            return filter(List.of("led", "color", "skin", "all"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("color") || args[0].equalsIgnoreCase("colour")) && canUse) {
            return filter(List.of("#A14DFF", "red", "blue", "green", "white", "black", "none"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("station") && canAdmin) {
            return filter(List.of("create", "remove", "list"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("station") && args[1].equalsIgnoreCase("remove") && canAdmin) {
            return filter(module.getWeaponCosmeticStationManager().stations().stream()
                    .map(WeaponCosmeticStationManager.StationRecord::id)
                    .toList(), args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("open")) && canAdmin) {
            return onlinePlayerNames(args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("skin") && canAdmin) {
            List<String> values = new ArrayList<>(availableSkinIds(sender));
            values.add(WeaponCosmeticManager.NONE);
            return filter(values, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin") && canAdmin) {
            return onlinePlayerNames(args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("skins") && canAdmin) {
            return filter(manager.getSkinnableWeaponIds(), args[1]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("token") && canAdmin) {
            return onlinePlayerNames(args[4]);
        }
        return List.of();
    }

    private List<String> availableSkinIds(CommandSender sender) {
        if (sender instanceof Player player) {
            String weaponId = manager.getWeaponId(player.getInventory().getItemInMainHand());
            if (weaponId != null) {
                List<String> ids = manager.getSkinIds(weaponId);
                if (!ids.isEmpty()) {
                    return ids;
                }
            }
        }
        return manager.getAllSkinIds();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private List<String> onlinePlayerNames(String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private boolean canUse(CommandSender sender) {
        return sender.hasPermission(USE_PERMISSION) || sender.hasPermission(ADMIN_PERMISSION);
    }
}
