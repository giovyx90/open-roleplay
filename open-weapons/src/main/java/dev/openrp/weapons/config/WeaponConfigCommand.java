package dev.openrp.weapons.config;

import dev.openrp.weapons.util.OpenPermissions;
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
            sender.sendMessage(Component.text("Ti serve " + PERMISSION + " per modificare la config armi.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("interfaccia")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Uso da console: /configarmi <leggi|imposta|rimuovi|ricarica|lista|campi|armatura|casco>");
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("armi")) {
                gui.openWeapons(player, 0);
            } else if (args.length >= 2 && matches(args[1], "protezioni", "protezione", "armatura", "casco")) {
                gui.openProtections(player, 0);
            } else {
                gui.openMain(player);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("armatura")) {
            return handleArmor(sender, args);
        }
        if (args[0].equalsIgnoreCase("casco")) {
            return handleHelmet(sender, args);
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "ricarica" -> {
                editor.reload();
                editor.reloadArmor();
                editor.reloadHelmet();
                sender.sendMessage(Component.text("weapons.yml e armor.yml ricaricati.", NamedTextColor.GREEN));
                return true;
            }
            case "lista" -> {
                sender.sendMessage(Component.text("Armi: " + String.join(", ", editor.weaponIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "campi" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Uso: /configarmi campi <arma>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Campi: " + editor.fieldsFor(args[1]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<nessuno>"), NamedTextColor.YELLOW));
                return true;
            }
            case "leggi" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /configarmi leggi <arma> <percorso>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.get(args[1], args[2]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "imposta" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /configarmi imposta <arma> <percorso> <valore>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                WeaponConfigEditor.EditResult result = editor.set(sender.getName(), args[1], args[2], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "rimuovi" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /configarmi rimuovi <arma> <percorso>", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Uso: /configarmi armatura <lista|campi|leggi|imposta|rimuovi|ricarica>", NamedTextColor.RED));
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "ricarica" -> {
                editor.reloadArmor();
                sender.sendMessage(Component.text("armor.yml ricaricato.", NamedTextColor.GREEN));
                return true;
            }
            case "lista" -> {
                sender.sendMessage(Component.text("Armature: " + String.join(", ", editor.armorIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "campi" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /configarmi armatura campi <armatura>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Campi armatura: " + editor.armorFieldsFor(args[2]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<nessuno>"), NamedTextColor.YELLOW));
                return true;
            }
            case "leggi" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /configarmi armatura leggi <armatura> <percorso>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.getArmor(args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "imposta" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Uso: /configarmi armatura imposta <armatura> <percorso> <valore>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                WeaponConfigEditor.EditResult result = editor.setArmor(sender.getName(), args[2], args[3], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "rimuovi" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /configarmi armatura rimuovi <armatura> <percorso>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.removeArmor(sender.getName(), args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Uso: /configarmi armatura <lista|campi|leggi|imposta|rimuovi|ricarica>", NamedTextColor.RED));
                return true;
            }
        }
    }

    private boolean handleHelmet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /configarmi casco <lista|campi|leggi|imposta|rimuovi|ricarica>", NamedTextColor.RED));
            return true;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "ricarica" -> {
                editor.reloadHelmet();
                sender.sendMessage(Component.text("caschi in armor.yml ricaricati.", NamedTextColor.GREEN));
                return true;
            }
            case "lista" -> {
                sender.sendMessage(Component.text("Caschi: " + String.join(", ", editor.helmetIds()), NamedTextColor.YELLOW));
                return true;
            }
            case "campi" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /configarmi casco campi <casco>", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Campi casco: " + editor.helmetFieldsFor(args[2]).stream()
                        .map(WeaponConfigEditor.FieldSpec::path)
                        .reduce((a, b) -> a + ", " + b).orElse("<nessuno>"), NamedTextColor.YELLOW));
                return true;
            }
            case "leggi" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /configarmi casco leggi <casco> <percorso>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.getHelmet(args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "imposta" -> {
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Uso: /configarmi casco imposta <casco> <percorso> <valore>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                WeaponConfigEditor.EditResult result = editor.setHelmet(sender.getName(), args[2], args[3], value);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            case "rimuovi" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /configarmi casco rimuovi <casco> <percorso>", NamedTextColor.RED));
                    return true;
                }
                WeaponConfigEditor.EditResult result = editor.removeHelmet(sender.getName(), args[2], args[3]);
                sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Uso: /configarmi casco <lista|campi|leggi|imposta|rimuovi|ricarica>", NamedTextColor.RED));
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
            return filter(List.of("interfaccia", "leggi", "imposta", "rimuovi", "ricarica", "lista", "campi", "armatura", "casco"), args[0]);
        }
        if (args[0].equalsIgnoreCase("interfaccia")) {
            if (args.length == 2) {
                return filter(List.of("armi", "protezioni"), args[1]);
            }
            return List.of();
        }
        if (args[0].equalsIgnoreCase("armatura")) {
            if (args.length == 2) {
                return filter(List.of("lista", "campi", "leggi", "imposta", "rimuovi", "ricarica"), args[1]);
            }
            if (args.length == 3 && matches(args[1], "leggi", "imposta", "rimuovi", "campi")) {
                return filter(editor.armorIds(), args[2]);
            }
            if (args.length == 4 && matches(args[1], "leggi", "imposta", "rimuovi")) {
                return filter(editor.armorFieldsFor(args[2]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[3]);
            }
            return List.of();
        }
        if (args[0].equalsIgnoreCase("casco")) {
            if (args.length == 2) {
                return filter(List.of("lista", "campi", "leggi", "imposta", "rimuovi", "ricarica"), args[1]);
            }
            if (args.length == 3 && matches(args[1], "leggi", "imposta", "rimuovi", "campi")) {
                return filter(editor.helmetIds(), args[2]);
            }
            if (args.length == 4 && matches(args[1], "leggi", "imposta", "rimuovi")) {
                return filter(editor.helmetFieldsFor(args[2]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[3]);
            }
            return List.of();
        }
        if (args.length == 2 && matches(args[0], "leggi", "imposta", "rimuovi", "campi")) {
            return filter(editor.weaponIds(), args[1]);
        }
        if (args.length == 3 && matches(args[0], "leggi", "imposta", "rimuovi")) {
            return filter(editor.fieldsFor(args[1]).stream().map(WeaponConfigEditor.FieldSpec::path).toList(), args[2]);
        }
        return List.of();
    }

    private boolean canEdit(CommandSender sender) {
        return OpenPermissions.hasAny(sender, PERMISSION, OpenPermissions.Weapons.ADMIN);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /configarmi <interfaccia|leggi|imposta|rimuovi|ricarica|lista|campi|armatura|casco>", NamedTextColor.RED));
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

    private boolean matches(String raw, String... aliases) {
        if (raw == null) {
            return false;
        }
        for (String alias : aliases) {
            if (raw.equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }
}
