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

public class SosGUI implements Listener {
   private static final int SLOT_POLICE = 11;
   private static final int SLOT_HOSPITAL = 13;
   private static final int SLOT_FIRE = 15;
   private final WeaponsModule module;

   public SosGUI(WeaponsModule module) {
      this.module = module;
   }

   public void open(Player player) {
      SosGUI.SosGUIHolder holder = new SosGUI.SosGUIHolder();
      Inventory gui = Bukkit.createInventory(holder, 27, NexoUI.getGlyphTitle("sos_gui", "SOS"));
      holder.setInventory(gui);
      ItemStack filler = NexoUI.getFiller();

      for (int i = 0; i < gui.getSize(); i++) {
         gui.setItem(i, filler);
      }

      gui.setItem(11, this.createServiceItem(Material.SHIELD, "Polizia", NamedTextColor.BLUE));
      gui.setItem(13, this.createServiceItem(Material.GOLDEN_APPLE, "Ospedale", NamedTextColor.GREEN));
      gui.setItem(15, this.createServiceItem(Material.CAMPFIRE, "Vigili del fuoco", NamedTextColor.RED));
      player.openInventory(gui);
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         Inventory topInv = event.getView().getTopInventory();
         if (topInv.getHolder() instanceof SosGUI.SosGUIHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == topInv) {
               switch (event.getRawSlot()) {
                  case 11:
                     this.module.getSosManager().startReasonInput(player, SosCall.Service.POLICE);
                  case 12:
                  case 14:
                  default:
                     break;
                  case 13:
                     this.module.getSosManager().startReasonInput(player, SosCall.Service.HOSPITAL);
                     break;
                  case 15:
                     this.module.getSosManager().startReasonInput(player, SosCall.Service.FIRE_DEPARTMENT);
               }
            }
         }
      }
   }

   private ItemStack createServiceItem(Material material, String name, NamedTextColor color) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text(name, color, new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false));
         meta.lore(List.of((TextComponent)Component.text("Clicca per chiamare " + name, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
         item.setItemMeta(meta);
      }

      return item;
   }

   public static class SosGUIHolder implements InventoryHolder {
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }

      public void setInventory(Inventory inventory) {
         this.inventory = inventory;
      }
   }
}
