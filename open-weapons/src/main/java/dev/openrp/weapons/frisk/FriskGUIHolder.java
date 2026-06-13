package dev.openrp.weapons.frisk;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class FriskGUIHolder implements InventoryHolder {
   private final UUID searcherUuid;
   private final UUID targetUuid;
   private Inventory inventory;

   public FriskGUIHolder(UUID searcherUuid, UUID targetUuid) {
      this.searcherUuid = searcherUuid;
      this.targetUuid = targetUuid;
   }

   public UUID getSearcherUuid() {
      return this.searcherUuid;
   }

   public UUID getTargetUuid() {
      return this.targetUuid;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   @NotNull
   public Inventory getInventory() {
      return this.inventory;
   }
}
