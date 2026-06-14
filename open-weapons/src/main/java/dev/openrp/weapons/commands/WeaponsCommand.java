package dev.openrp.weapons.commands;

import dev.openrp.weapons.util.OpenPermissions;
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
            sender.sendMessage("Solo i giocatori possono usare questo comando.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!OpenPermissions.hasAny(player,
                    OpenPermissions.Weapons.DEBUG,
                    OpenPermissions.Weapons.ADMIN,
                    OpenPermissions.Staff.RELOAD)) {
                player.sendMessage(Component.text("Ti serve openrp.weapons.debug o openrp.staff.reload per ricaricare le armi.", NamedTextColor.RED));
                return true;
            }
            module.getWeaponRegistry().load(new java.io.File(module.getCore().getDataFolder(), "weapons.yml"));
            module.getAmmoRegistry().load(new java.io.File(module.getCore().getDataFolder(), "ammo.yml"));
            player.sendMessage(Component.text("Configurazione armi e munizioni ricaricata.", NamedTextColor.GREEN));
            return true;
        }

        if (!OpenPermissions.hasAny(player,
                OpenPermissions.Weapons.VIEW,
                OpenPermissions.Weapons.GIVE,
                OpenPermissions.Weapons.ADMIN,
                OpenPermissions.Test.ITEMS)) {
            player.sendMessage(Component.text("Ti serve openrp.weapons.view per aprire questo catalogo.", NamedTextColor.RED));
            return true;
        }

        // Open GUI
        gui.open(player);
        return true;
    }
}
