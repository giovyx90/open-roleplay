package dev.openrp.weapons.frisk;

import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class FriskListener implements Listener {
   private final WeaponsModule module;

   public FriskListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.cleanTags(event.getPlayer());
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      this.cleanTags(event.getEntity());
   }

   private void cleanTags(Player player) {
      for (Entity passenger : player.getPassengers()) {
         if (passenger instanceof TextDisplay display) {
            String text = PlainTextComponentSerializer.plainText().serialize(display.text());
            String normalized = text.toUpperCase(java.util.Locale.ROOT);
            if (normalized.contains("ARRESTABILE")
               || normalized.contains("ABBATTIBILE")) {
               display.remove();
            }
         }
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      Inventory topInv = event.getView().getTopInventory();
      if (topInv.getHolder() instanceof FriskGUIHolder
         && (event.getClickedInventory() == topInv || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent event) {
      Inventory topInv = event.getView().getTopInventory();
      if (topInv.getHolder() instanceof FriskGUIHolder) {
         for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topInv.getSize()) {
               event.setCancelled(true);
               return;
            }
         }
      }
   }
}
