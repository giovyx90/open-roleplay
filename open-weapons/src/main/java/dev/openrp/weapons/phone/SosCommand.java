package dev.openrp.weapons.phone;

import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SosCommand implements CommandExecutor {
   private final WeaponsModule module;

   public SosCommand(WeaponsModule module) {
      this.module = module;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player player) {
         if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /sos <gps|respond>", NamedTextColor.RED));
            return true;
         }

         switch (args[0].toLowerCase()) {
            case "gps":
               if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
                  this.module.getSosManager().stopGps(player, true);
                  return true;
               }

               if (args.length < 2) {
                  player.sendMessage(Component.text("Usage: /sos gps <callId|stop>", NamedTextColor.RED));
                  return true;
               }

               this.module.getSosManager().activateGps(player, args[1]);
               break;
            case "respond":
               if (args.length < 2) {
                  player.sendMessage(Component.text("Usage: /sos respond <callId>", NamedTextColor.RED));
                  return true;
               }

               this.module.getSosManager().startResponseInput(player, args[1]);
               break;
            default:
               player.sendMessage(Component.text("Usage: /sos <gps|respond>", NamedTextColor.RED));
         }

         return true;
      } else {
         sender.sendMessage(Component.text("Only players can use SOS actions.", NamedTextColor.RED));
         return true;
      }
   }
}
