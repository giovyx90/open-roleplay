package dev.openrp.weapons.phone;

import dev.openrp.weapons.module.WeaponsModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PhoneListener implements Listener {
   private final WeaponsModule module;

   public PhoneListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Action action = event.getAction();
         if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (this.module.getMobilePhoneManager().isMobilePhone(item)) {
               event.setCancelled(true);
               this.module.getPhoneGUI().open(event.getPlayer());
            }
         }
      }
   }
}
