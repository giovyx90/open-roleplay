package dev.openrp.weapons.robbery;

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
         boolean created = this.module.getLootboxBridge()
            .createRobberyQuitLootbox(player, session.getRobberUuid(), robberName, player.getLocation());
         if (!created) {
            this.module.getCore().getLogger().warning("[Robbery] NEXTLootbox non disponibile - impossibile creare la lootbox di revisione per " + player.getName());
            return;
         }
         rm.markPendingSlog(player.getUniqueId(), session.getRobberUuid());
         if (robber != null && robber.isOnline()) {
            robber.sendMessage(Component.text(player.getName() + " e' uscito dal gioco durante la rapina! La sua lootbox e' in attesa di revisione staff.", NamedTextColor.GREEN));
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
               () -> player.sendMessage(Component.text("Sei uscito durante una rapina! I tuoi oggetti sono stati lasciati in una lootbox.", NamedTextColor.RED)),
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
