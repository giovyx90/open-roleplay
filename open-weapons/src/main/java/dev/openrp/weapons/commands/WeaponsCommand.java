package dev.openrp.weapons.commands;

import it.meridian.core.permissions.NextPermissions;
import dev.openrp.weapons.gui.WeaponsGUI;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WeaponsCommand implements CommandExecutor {
    private final WeaponsModule module;
    private final WeaponsGUI gui;

    public WeaponsCommand(WeaponsModule module, WeaponsGUI gui) {
        this.module = module;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!NextPermissions.hasAny(player,
                    NextPermissions.Weapons.DEBUG,
                    NextPermissions.Weapons.ADMIN,
                    NextPermissions.Staff.RELOAD)) {
                player.sendMessage(Component.text("You need openrp.weapons.debug or openrp.staff.reload to reload weapons.", NamedTextColor.RED));
                return true;
            }
            module.getWeaponRegistry().load(new java.io.File(module.getCore().getDataFolder(), "weapons.yml"));
            module.getAmmoRegistry().load(new java.io.File(module.getCore().getDataFolder(), "ammo.yml"));
            player.sendMessage(Component.text("Weapons and Ammo configuration reloaded.", NamedTextColor.GREEN));
            return true;
        }

        if (!NextPermissions.hasAny(player,
                NextPermissions.Weapons.VIEW,
                NextPermissions.Weapons.GIVE,
                NextPermissions.Weapons.ADMIN,
                NextPermissions.Test.ITEMS)) {
            player.sendMessage(Component.text("You need openrp.weapons.view to open this catalog.", NamedTextColor.RED));
            return true;
        }

        // Open GUI
        gui.open(player);
        return true;
    }
}
