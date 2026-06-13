package dev.openrp.weapons.magazine;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.WeaponDefinition;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MagazineManager {
   private final NamespacedKey weaponIdKey;
   private final NamespacedKey ammoTypeKey;
   private final NamespacedKey ammoCountKey;
   private final NamespacedKey capacityKey;
   private final NamespacedKey uniqueIdKey;

   public MagazineManager(CorePlugin core) {
      this.weaponIdKey = new NamespacedKey(core, "magazine_weapon_id");
      this.ammoTypeKey = new NamespacedKey(core, "magazine_ammo_type");
      this.ammoCountKey = new NamespacedKey(core, "magazine_ammo_count");
      this.capacityKey = new NamespacedKey(core, "magazine_capacity");
      this.uniqueIdKey = new NamespacedKey(core, "magazine_uid");
   }

   public boolean isMagazine(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.weaponIdKey, PersistentDataType.STRING) : false;
   }

   public String getWeaponId(ItemStack item) {
      return !this.isMagazine(item) ? null : (String)item.getItemMeta().getPersistentDataContainer().get(this.weaponIdKey, PersistentDataType.STRING);
   }

   public String getAmmoType(ItemStack item) {
      return !this.isMagazine(item) ? null : (String)item.getItemMeta().getPersistentDataContainer().get(this.ammoTypeKey, PersistentDataType.STRING);
   }

   public int getAmmoCount(ItemStack item) {
      return !this.isMagazine(item)
         ? 0
         : (Integer)item.getItemMeta().getPersistentDataContainer().getOrDefault(this.ammoCountKey, PersistentDataType.INTEGER, 0);
   }

   public int getCapacity(ItemStack item) {
      return !this.isMagazine(item)
         ? 0
         : (Integer)item.getItemMeta().getPersistentDataContainer().getOrDefault(this.capacityKey, PersistentDataType.INTEGER, 0);
   }

   public ItemStack createMagazine(WeaponDefinition weapon, int ammoCount) {
      ItemStack item = new ItemStack(Material.IRON_NUGGET);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         int clampedAmmo = Math.max(0, Math.min(ammoCount, weapon.getMagazineSize()));
         meta.displayName(
            Component.text(weapon.getDisplayName() + " Magazine", NamedTextColor.GRAY)
               .decoration(TextDecoration.BOLD, false)
               .decoration(TextDecoration.ITALIC, false)
         );
         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         pdc.set(this.weaponIdKey, PersistentDataType.STRING, weapon.getId());
         pdc.set(this.ammoTypeKey, PersistentDataType.STRING, weapon.getAmmoType());
         pdc.set(this.ammoCountKey, PersistentDataType.INTEGER, clampedAmmo);
         pdc.set(this.capacityKey, PersistentDataType.INTEGER, weapon.getMagazineSize());
         pdc.set(this.uniqueIdKey, PersistentDataType.STRING, UUID.randomUUID().toString());
         int magazineModelData = weapon.getMagazineModelData() > 0
            ? weapon.getMagazineModelData()
            : defaultMagazineModelData(weapon.getId());
         if (magazineModelData > 0) {
            meta.setCustomModelData(magazineModelData);
         }
         meta.lore(this.buildLore(weapon, clampedAmmo, weapon.getMagazineSize()));
         item.setItemMeta(meta);
      }

      return item;
   }

   public void setAmmoCount(ItemStack item, WeaponDefinition weapon, int ammoCount) {
      if (this.isMagazine(item)) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            int capacity = this.getCapacity(item);
            int clampedAmmo = Math.max(0, Math.min(ammoCount, capacity));
            meta.getPersistentDataContainer().set(this.ammoCountKey, PersistentDataType.INTEGER, clampedAmmo);
            meta.lore(this.buildLore(weapon, clampedAmmo, capacity));
            item.setItemMeta(meta);
         }
      }
   }

   private List<Component> buildLore(WeaponDefinition weapon, int ammoCount, int capacity) {
      return List.of(
         ((TextComponent)Component.text("Weapon: ", NamedTextColor.GRAY).append(Component.text(weapon.getDisplayName(), NamedTextColor.WHITE)))
            .decoration(TextDecoration.ITALIC, false),
         ((TextComponent)Component.text("Ammo: ", NamedTextColor.GRAY).append(Component.text(ammoCount + " / " + capacity, NamedTextColor.YELLOW)))
            .decoration(TextDecoration.ITALIC, false),
         ((TextComponent)Component.text("Caliber: ", NamedTextColor.GRAY).append(Component.text(weapon.getAmmoType(), NamedTextColor.WHITE)))
            .decoration(TextDecoration.ITALIC, false)
      );
   }

   private int defaultMagazineModelData(String weaponId) {
      if (weaponId == null) {
         return 0;
      }
      return switch (weaponId) {
         case "ak47", "ak_47" -> 5;
         case "ar15", "ar_15" -> 7;
         case "fn_scar_h" -> 13;
         case "m4", "m4a1" -> 15;
         case "mp5" -> 16;
         case "sig_mcx_semi", "sig_mcx_semiauto", "sig_mcx_assault" -> 17;
         case "hk416" -> 18;
         case "mp7" -> 19;
         case "barrett_mrad" -> 20;
         case "awp" -> 21;
         case "famas" -> 22;
         case "ppk" -> 23;
         case "glock19", "glock_19" -> 24;
         case "beretta_92fs" -> 27;
         case "desert_eagle" -> 29;
         case "ghost_pistol" -> 30;
         case "ghost_rifle" -> 31;
         case "ghost_smg" -> 32;
         default -> 0;
      };
   }

}
