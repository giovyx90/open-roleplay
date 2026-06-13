package dev.openrp.weapons.robbery;

import it.meridian.core.lootbox.LootboxServiceRegistry;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RobListener implements Listener {
   private final WeaponsModule module;

   public RobListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      RobberyManager rm = this.module.getRobberyManager();
      if (rm.isBeingRobbed(player.getUniqueId())) {
         RobberySession session = rm.getSession(player.getUniqueId());
         rm.endRobbery(player.getUniqueId());
         Player robber = this.module.getCore().getServer().getPlayer(session.getRobberUuid());
         String robberName = robber != null ? robber.getName() : session.getRobberUuid().toString();
         boolean created = LootboxServiceRegistry.get()
            .map(service -> service.createRobberyQuitLootbox(player, session.getRobberUuid(), robberName, player.getLocation()))
            .orElse(false);
         if (!created) {
            this.module.getCore().getLogger().warning("[Robbery] NEXTLootbox unavailable - could not create review lootbox for " + player.getName());
            return;
         }
         rm.markPendingSlog(player.getUniqueId(), session.getRobberUuid());
         if (robber != null && robber.isOnline()) {
            robber.sendMessage(Component.text(player.getName() + " fled the game during the robbery! Their lootbox is pending staff review.", NamedTextColor.GREEN));
         }
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      RobberyManager rm = this.module.getRobberyManager();
      if (rm.getPendingSlogRobber(player.getUniqueId()) != null) {
         this.module
            .getCore()
            .getServer()
            .getScheduler()
            .runTaskLater(
               this.module.getCore(),
               () -> player.sendMessage(Component.text("You left during a robbery! Your items have been dropped as a lootbox.", NamedTextColor.RED)),
               20L
            );
      }
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      RobberyManager rm = this.module.getRobberyManager();
      if (rm.isBeingRobbed(player.getUniqueId())) {
         rm.endRobbery(player.getUniqueId());
      }
   }
}
