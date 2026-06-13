package dev.openrp.weapons.attachments;

import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AttachmentWorkbenchCommand implements CommandExecutor {
    private final WeaponsModule module;

    public AttachmentWorkbenchCommand(WeaponsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("openrp.weapons.workbench")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        module.getAttachmentWorkbenchGUI().open(player);
        return true;
    }
}
