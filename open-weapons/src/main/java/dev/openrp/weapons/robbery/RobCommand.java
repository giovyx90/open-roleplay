package dev.openrp.weapons.robbery;

import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RobCommand implements CommandExecutor {
   private final WeaponsModule module;

   public RobCommand(WeaponsModule module) {
      this.module = module;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player robber) {
         if (!robber.hasPermission("openrp.rob.use")) {
            robber.sendMessage(Component.text("You don't have permission to rob players.", NamedTextColor.RED));
            return true;
         }

         if (args.length == 0) {
            robber.sendMessage(Component.text("Usage: /rob <player>", NamedTextColor.RED));
            return true;
         }

         Player victim = Bukkit.getPlayer(args[0]);
         if (victim != null && victim.isOnline()) {
            if (victim.getUniqueId().equals(robber.getUniqueId())) {
               robber.sendMessage(Component.text("You cannot rob yourself.", NamedTextColor.RED));
               return true;
            } else if (robber.getLocation().distance(victim.getLocation()) > 5.0) {
               robber.sendMessage(Component.text("You are too far from " + victim.getName() + " (max 5 blocks).", NamedTextColor.RED));
               return true;
            } else {
               ItemStack itemInHand = robber.getInventory().getItemInMainHand();
               WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(itemInHand);
               if (weapon == null) {
                  robber.sendMessage(Component.text("You must hold a weapon (firearm or melee) to rob someone.", NamedTextColor.RED));
                  return true;
               } else {
                  RobberyManager rm = this.module.getRobberyManager();
                  if (rm.isBeingRobbed(victim.getUniqueId())) {
                     robber.sendMessage(Component.text("This player is already being robbed.", NamedTextColor.RED));
                     return true;
                  } else if (rm.hasActiveRobbery(robber.getUniqueId())) {
                     robber.sendMessage(Component.text("You are already robbing someone else.", NamedTextColor.RED));
                     return true;
                  } else if (!rm.canRobToday(robber)) {
                     robber.sendMessage(Component.text("You have reached your daily limit for robberies (2 per day).", NamedTextColor.RED));
                     return true;
                  } else {
                     rm.startRobbery(robber, victim);
                     robber.sendMessage(Component.text("You are now robbing " + victim.getName() + ". You have 5 minutes.", NamedTextColor.GREEN));
                     victim.sendMessage(
                        Component.text(
                           "You are being robbed! Do what the criminal says, or you will become killable. If you disconnect, your inventory will be given to the robber.",
                           NamedTextColor.RED
                        )
                     );
                     return true;
                  }
               }
            }
         } else {
            robber.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return true;
         }
      } else {
         sender.sendMessage("Only players can use this command.");
         return true;
      }
   }
}
