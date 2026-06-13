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
            robber.sendMessage(Component.text("Non hai il permesso di rapinare giocatori.", NamedTextColor.RED));
            return true;
         }

         if (args.length == 0) {
            robber.sendMessage(Component.text("Uso: /rob <giocatore>", NamedTextColor.RED));
            return true;
         }

         Player victim = Bukkit.getPlayer(args[0]);
         if (victim != null && victim.isOnline()) {
            if (victim.getUniqueId().equals(robber.getUniqueId())) {
               robber.sendMessage(Component.text("Non puoi rapinare te stesso.", NamedTextColor.RED));
               return true;
            } else if (robber.getLocation().distance(victim.getLocation()) > 5.0) {
               robber.sendMessage(Component.text("Sei troppo lontano da " + victim.getName() + " (massimo 5 blocchi).", NamedTextColor.RED));
               return true;
            } else {
               ItemStack itemInHand = robber.getInventory().getItemInMainHand();
               WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(itemInHand);
               if (weapon == null) {
                  robber.sendMessage(Component.text("Devi impugnare un'arma, da fuoco o melee, per rapinare qualcuno.", NamedTextColor.RED));
                  return true;
               } else {
                  RobberyManager rm = this.module.getRobberyManager();
                  if (rm.isBeingRobbed(victim.getUniqueId())) {
                     robber.sendMessage(Component.text("Questo giocatore e' gia' sotto rapina.", NamedTextColor.RED));
                     return true;
                  } else if (rm.hasActiveRobbery(robber.getUniqueId())) {
                     robber.sendMessage(Component.text("Stai gia' rapinando qualcun altro.", NamedTextColor.RED));
                     return true;
                  } else if (!rm.canRobToday(robber)) {
                     robber.sendMessage(Component.text("Hai raggiunto il limite giornaliero di rapine (2 al giorno).", NamedTextColor.RED));
                     return true;
                  } else {
                     rm.startRobbery(robber, victim);
                     robber.sendMessage(Component.text("Stai rapinando " + victim.getName() + ". Hai 5 minuti.", NamedTextColor.GREEN));
                     victim.sendMessage(
                        Component.text(
                           "Sei sotto rapina! Fai cio' che dice il criminale o diventerai uccidibile. Se ti disconnetti, il tuo inventario verra' dato al rapinatore.",
                           NamedTextColor.RED
                        )
                     );
                     return true;
                  }
               }
            }
         } else {
            robber.sendMessage(Component.text("Giocatore non trovato o non online.", NamedTextColor.RED));
            return true;
         }
      } else {
         sender.sendMessage("Solo i giocatori possono usare questo comando.");
         return true;
      }
   }
}
