package dev.openrp.weapons.config;

import it.meridian.core.permissions.NextPermissions;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class WeaponConfigCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "openrp.weapons.config";

    private final WeaponsModule module;
    private final WeaponConfigEditor editor;
    private final WeaponConfigGUI gui;

    public WeaponConfigCommand(WeaponsModule module, WeaponConfigEditor editor, WeaponConfigGUI gui) {
        this.module = module;
        this.editor = editor;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!canEdit(sender)) {
            sender.sendMessage(Component.text("You need " + PERMISSION + " to edit weapon config.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console usage: /weaponconfig <get|set|remove|reload|list|fields|armor|helmet>");
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("weapons")) {
                gui.openWeapons(player, 0);
            } else if (args.length >= 2 && List.of("protections", "protection", "armor", "helmets", "helmet").contains(args[1].toLowerCase(Locale.ROOT))) {
                gui.openProtections(player, 0);
            } else {
                gui.openMain(player);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("armor")) {
            return handleArmor(sender, args);
        }
        if (args[0].equalsIgnoreCase("helmet")) {
            return handleHelmet(sender, args);
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                editor.reload();
                editor.reloadArmor();
                editor.reloadHelmet();
                sender.sendMessage(Component.text("weapons.yml and armor.yml reloaded.", NamedTextColor.GREEN));
                return true;
            }
            case "list" -> {
                sender.sendMessage(Component.text("Weapons: " + String.join(", ", editor.weaponIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "fields" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig fields <weapon>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Fields: " + editor.fieldsFor(args[1]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<none>"), NamedTextColor.YELLOW));
                return true;
            }
            case "get" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig get <weapon> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.get(args[1], args[2]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig set <weapon> <path> <value>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                WeaponConfigEditor.EditResult result = editor.set(sender.getName(), args[1], args[2], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig remove <weapon> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.remove(sender.getName(), args[1], args[2]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    private boolean handleArmor(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weaponconfig armor <list|fields|get|set|remove|reload>", NamedTextColor.RED));
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                editor.reloadArmor();
                sender.sendMessage(Component.text("armor.yml reloaded.", NamedTextColor.GREEN));
                return true;
            }
            case "list" -> {
                sender.sendMessage(Component.text("Armor: " + String.join(", ", editor.armorIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "fields" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig armor fields <armor>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Armor fields: " + editor.armorFieldsFor(args[2]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<none>"), NamedTextColor.YELLOW));
                return true;
            }
            case "get" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig armor get <armor> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.getArmor(args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig armor set <armor> <path> <value>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                WeaponConfigEditor.EditResult result = editor.setArmor(sender.getName(), args[2], args[3], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig armor remove <armor> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.removeArmor(sender.getName(), args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Usage: /weaponconfig armor <list|fields|get|set|remove|reload>", NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean handleHelmet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weaponconfig helmet <list|fields|get|set|remove|reload>", NamedTextColor.RED));
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                editor.reloadHelmet();
                sender.sendMessage(Component.text("armor.yml helmets reloaded.", NamedTextColor.GREEN));
                return true;
            }
            case "list" -> {
                sender.sendMessage(Component.text("Helmets: " + String.join(", ", editor.helmetIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "fields" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig helmet fields <helmet>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Helmet fields: " + editor.helmetFieldsFor(args[2]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<none>"), NamedTextColor.YELLOW));
                return true;
            }
            case "get" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig helmet get <helmet> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.getHelmet(args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig helmet set <helmet> <path> <value>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                WeaponConfigEditor.EditResult result = editor.setHelmet(sender.getName(), args[2], args[3], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /weaponconfig helmet remove <helmet> <path>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.removeHelmet(sender.getName(), args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Usage: /weaponconfig helmet <list|fields|get|set|remove|reload>", NamedTextColor.RED));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!canEdit(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("gui", "get", "set", "remove", "reload", "list", "fields", "armor", "helmet"), args[0]);
        }
        if (args[0].equalsIgnoreCase("gui")) {
            if (args.length == 2) {
                return filter(List.of("weapons", "protections", "armor", "helmets"), args[1]);
            }
            return List.of();
        }
        if (args[0].equalsIgnoreCase("armor")) {
            if (args.length == 2) {
                return filter(List.of("list", "fields", "get", "set", "remove", "reload"), args[1]);
            }
            if (args.length == 3 && List.of("get", "set", "remove", "fields").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filter(editor.armorIds(), args[2]);
            }
            if (args.length == 4 && List.of("get", "set", "remove").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filter(editor.armorFieldsFor(args[2]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[3]);
            }
            return List.of();
        }
        if (args[0].equalsIgnoreCase("helmet")) {
            if (args.length == 2) {
                return filter(List.of("list", "fields", "get", "set", "remove", "reload"), args[1]);
            }
            if (args.length == 3 && List.of("get", "set", "remove", "fields").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filter(editor.helmetIds(), args[2]);
            }
            if (args.length == 4 && List.of("get", "set", "remove").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filter(editor.helmetFieldsFor(args[2]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[3]);
            }
            return List.of();
        }
        if (args.length == 2 && List.of("get", "set", "remove", "fields").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(editor.weaponIds(), args[1]);
        }
        if (args.length == 3 && List.of("get", "set", "remove").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(editor.fieldsFor(args[1]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[2]);
        }
        return List.of();
    }

    private boolean canEdit(CommandSender sender) {
        return NextPermissions.hasAny(sender, PERMISSION, NextPermissions.Weapons.ADMIN);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /weaponconfig <gui|get|set|remove|reload|list|fields|armor|helmet>", NamedTextColor.RED));
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
            if (result.size() >= 50) {
                break;
            }
        }
        return result;
    }
}
