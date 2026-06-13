package dev.openrp.weapons.actions;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class QuickActionHolder implements InventoryHolder {
   private final UUID actorUuid;
   private final UUID targetUuid;
   private Inventory inventory;

   public QuickActionHolder(UUID actorUuid, UUID targetUuid) {
      this.actorUuid = actorUuid;
      this.targetUuid = targetUuid;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public UUID getActorUuid() {
      return this.actorUuid;
   }

   public UUID getTargetUuid() {
      return this.targetUuid;
   }
}
