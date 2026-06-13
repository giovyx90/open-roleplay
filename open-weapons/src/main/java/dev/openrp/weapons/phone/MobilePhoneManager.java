package dev.openrp.weapons.phone;

import it.meridian.core.CorePlugin;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MobilePhoneManager {
   private final CorePlugin core;
   private final NamespacedKey phoneKey;

   public MobilePhoneManager(CorePlugin core) {
      this.core = core;
      this.phoneKey = new NamespacedKey(core, "mobile_phone");
   }

   public ItemStack createMobilePhone() {
      ItemStack item = new ItemStack(Material.PAPER);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.text("Telefono cellulare", NamedTextColor.GRAY)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false));
         meta.setCustomModelData(163);
         meta.getPersistentDataContainer().set(this.phoneKey, PersistentDataType.BYTE, (byte)1);
         List<Component> lore = new ArrayList<>();
         lore.add(Component.text(""));
         lore.add(Component.text("Richiesto per la radio aziendale", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("Clic destro per aprire le app", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("Usa /c <azienda> per chattare", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
         meta.lore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   public boolean isMobilePhone(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.phoneKey, PersistentDataType.BYTE) : false;
   }

   public NamespacedKey getPhoneKey() {
      return this.phoneKey;
   }
}
