package dev.openrp.weapons.phone;

import it.meridian.core.gui.NexoUI;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PhoneGUI implements Listener {
   private static final int SLOT_SOS = 13;
   private final WeaponsModule module;

   public PhoneGUI(WeaponsModule module) {
      this.module = module;
   }

   public void open(Player player) {
      PhoneGUI.PhoneGUIHolder holder = new PhoneGUI.PhoneGUIHolder();
      Inventory gui = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("phone_apps_gui", "Phone Apps"));
      holder.setInventory(gui);
      ItemStack filler = NexoUI.getFiller();

      for (int i = 0; i < gui.getSize(); i++) {
         gui.setItem(i, filler);
      }

      gui.setItem(13, this.createSosApp());
      player.openInventory(gui);
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         Inventory topInv = event.getView().getTopInventory();
         if (topInv.getHolder() instanceof PhoneGUI.PhoneGUIHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
               if (event.getRawSlot() == 13) {
                  this.module.getSosGUI().open(player);
               }
            }
         }
      }
   }

   private ItemStack createSosApp() {
      ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text("SOS", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false));
         meta.lore(List.of((TextComponent)Component.text("Open emergency services", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
         meta.setEnchantmentGlintOverride(true);
         item.setItemMeta(meta);
      }

      return item;
   }

   public static class PhoneGUIHolder implements InventoryHolder {
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }

      public void setInventory(Inventory inventory) {
         this.inventory = inventory;
      }
   }
}
