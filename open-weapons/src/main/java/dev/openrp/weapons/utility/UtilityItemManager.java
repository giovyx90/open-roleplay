package dev.openrp.weapons.utility;

import it.meridian.core.CorePlugin;
import it.meridian.core.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class UtilityItemManager {
   public static final int PARACHUTE_OPEN_MODEL_DATA = 194;
   public static final int FIRE_AXE_BROKEN_MODEL_DATA = 116;
   public static final int EXTINGUISHER_MAX_CHARGES = 100;
   public static final int PEPPER_SPRAY_MAX_USES = 30;
   private final UtilitySettings settings;
   private final NamespacedKey utilityKey;
   private final NamespacedKey duffelContentsKey;
   private final NamespacedKey extinguisherChargesKey;
   private final NamespacedKey fireAxeUsesKey;
   private final NamespacedKey fingerprintDataKey;
   private final NamespacedKey pepperSprayUsesKey;
   private final NamespacedKey gagUsesKey;
   private final NamespacedKey blindfoldUsesKey;

   public UtilityItemManager(CorePlugin core, UtilitySettings settings) {
      this.settings = settings == null ? UtilitySettings.defaults() : settings;
      this.utilityKey = new NamespacedKey(core, "utility_item");
      this.duffelContentsKey = new NamespacedKey(core, "duffel_contents");
      this.extinguisherChargesKey = new NamespacedKey(core, "extinguisher_charges");
      this.fireAxeUsesKey = new NamespacedKey(core, "fire_axe_uses");
      this.fingerprintDataKey = new NamespacedKey(core, "fingerprint_data");
      this.pepperSprayUsesKey = new NamespacedKey(core, "pepper_spray_uses");
      this.gagUsesKey = new NamespacedKey(core, "gag_uses");
      this.blindfoldUsesKey = new NamespacedKey(core, "blindfold_uses");
   }

   public ItemStack createItem(UtilityItemType type) {
      ItemStack item = new ItemBuilder(materialFor(type))
         .name(Component.text(type.getDisplayName(), NamedTextColor.GRAY)
            .decoration(TextDecoration.BOLD, false)
            .decoration(TextDecoration.ITALIC, false))
         .lore(this.buildLore(type))
         .build();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().set(this.utilityKey, PersistentDataType.STRING, type.getId());
         if (type.getCustomModelData() > 0) {
            meta.setCustomModelData(type.getCustomModelData());
         }
         if (type == UtilityItemType.FIRE_EXTINGUISHER) {
            meta.getPersistentDataContainer().set(this.extinguisherChargesKey, PersistentDataType.INTEGER, 100);
         } else if (type == UtilityItemType.FIRE_AXE) {
            meta.getPersistentDataContainer().set(this.fireAxeUsesKey, PersistentDataType.INTEGER, settings.fireAxeUses());
            meta.lore(this.buildLore(UtilityItemType.FIRE_AXE, "Durabilita': " + settings.fireAxeUses() + "/" + settings.fireAxeUses()));
         } else if (type == UtilityItemType.PAINT_SPRAY) {
            meta.getPersistentDataContainer().set(this.pepperSprayUsesKey, PersistentDataType.INTEGER, 30);
         } else if (type == UtilityItemType.GAG) {
            meta.getPersistentDataContainer().set(this.gagUsesKey, PersistentDataType.INTEGER, settings.gagUses());
            meta.lore(this.buildLore(UtilityItemType.GAG, "Usi: " + settings.gagUses() + "/" + settings.gagUses()));
         } else if (type == UtilityItemType.BLINDFOLD) {
            meta.getPersistentDataContainer().set(this.blindfoldUsesKey, PersistentDataType.INTEGER, settings.blindfoldUses());
            meta.lore(this.buildLore(UtilityItemType.BLINDFOLD, "Usi: " + settings.blindfoldUses() + "/" + settings.blindfoldUses()));
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private org.bukkit.Material materialFor(UtilityItemType type) {
      return type == UtilityItemType.FIRE_AXE ? settings.fireAxeMaterial() : type.getMaterial();
   }

   public boolean isUtilityItem(ItemStack item) {
      return this.getType(item) != null;
   }

   public boolean isType(ItemStack item, UtilityItemType type) {
      return this.getType(item) == type;
   }

   public UtilityItemType getType(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         String id = (String)item.getItemMeta().getPersistentDataContainer().get(this.utilityKey, PersistentDataType.STRING);
         return UtilityItemType.fromId(id);
      } else {
         return null;
      }
   }

   public boolean isWearing(Player player, UtilityItemType type) {
      return player != null && this.isType(player.getInventory().getHelmet(), type);
   }

   public NamespacedKey getDuffelContentsKey() {
      return this.duffelContentsKey;
   }

   public NamespacedKey getExtinguisherChargesKey() {
      return this.extinguisherChargesKey;
   }

   public NamespacedKey getFireAxeUsesKey() {
      return this.fireAxeUsesKey;
   }

   public NamespacedKey getFingerprintDataKey() {
      return this.fingerprintDataKey;
   }

   public NamespacedKey getPepperSprayUsesKey() {
      return this.pepperSprayUsesKey;
   }

   public int getExtinguisherCharges(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Integer value = (Integer)item.getItemMeta().getPersistentDataContainer().get(this.extinguisherChargesKey, PersistentDataType.INTEGER);
         return value == null ? 100 : Math.max(0, value);
      } else {
         return 0;
      }
   }

   public void setExtinguisherCharges(ItemStack item, int charges) {
      if (item != null && item.hasItemMeta()) {
         int clamped = Math.max(0, Math.min(100, charges));
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.extinguisherChargesKey, PersistentDataType.INTEGER, clamped);
         meta.lore(this.buildLore(UtilityItemType.FIRE_EXTINGUISHER, "Durabilita': " + clamped + "/100"));
         item.setItemMeta(meta);
      }
   }

   public int getPepperSprayUses(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Integer value = (Integer)item.getItemMeta().getPersistentDataContainer().get(this.pepperSprayUsesKey, PersistentDataType.INTEGER);
         return value == null ? 30 : Math.max(0, value);
      } else {
         return 0;
      }
   }

   public void setPepperSprayUses(ItemStack item, int uses) {
      if (item != null && item.hasItemMeta()) {
         int clamped = Math.max(0, Math.min(30, uses));
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.pepperSprayUsesKey, PersistentDataType.INTEGER, clamped);
         meta.lore(this.buildLore(UtilityItemType.PAINT_SPRAY, "Usi: " + clamped + "/30"));
         item.setItemMeta(meta);
      }
   }

   public int getFireAxeUses(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         Integer value = (Integer)item.getItemMeta().getPersistentDataContainer().get(this.fireAxeUsesKey, PersistentDataType.INTEGER);
         return value == null ? settings.fireAxeUses() : Math.max(0, value);
      } else {
         return 0;
      }
   }

   public void setFireAxeUses(ItemStack item, int uses) {
      if (item != null && item.hasItemMeta()) {
         int maxUses = settings.fireAxeUses();
         int clamped = Math.max(0, Math.min(maxUses, uses));
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.fireAxeUsesKey, PersistentDataType.INTEGER, clamped);
         if (clamped <= 0) {
            meta.displayName(
               Component.text("Ascia antincendio rotta", NamedTextColor.GRAY)
                  .decoration(TextDecoration.BOLD, false)
                  .decoration(TextDecoration.ITALIC, false)
            );
         }

         meta.lore(this.buildLore(UtilityItemType.FIRE_AXE, "Durabilita': " + clamped + "/" + maxUses));
         item.setItemMeta(meta);
      }
   }

   public int getRestraintUses(ItemStack item, UtilityItemType type) {
      NamespacedKey key = restraintUsesKey(type);
      if (key == null || item == null || !item.hasItemMeta()) {
         return 0;
      }
      int defaultUses = defaultRestraintUses(type);
      Integer value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
      return value == null ? defaultUses : Math.max(0, value);
   }

   public void setRestraintUses(ItemStack item, UtilityItemType type, int uses) {
      NamespacedKey key = restraintUsesKey(type);
      if (key == null || item == null || !item.hasItemMeta()) {
         return;
      }
      int maxUses = defaultRestraintUses(type);
      int clamped = Math.max(0, Math.min(maxUses, uses));
      ItemMeta meta = item.getItemMeta();
      meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, clamped);
      meta.lore(this.buildLore(type, "Usi: " + clamped + "/" + maxUses));
      item.setItemMeta(meta);
   }

   public boolean consumeRestraintUse(ItemStack item, UtilityItemType type) {
      int uses = getRestraintUses(item, type);
      if (uses <= 0) {
         return false;
      }
      setRestraintUses(item, type, uses - 1);
      return true;
   }

   private NamespacedKey restraintUsesKey(UtilityItemType type) {
      return switch (type) {
         case GAG -> gagUsesKey;
         case BLINDFOLD -> blindfoldUsesKey;
         default -> null;
      };
   }

   private int defaultRestraintUses(UtilityItemType type) {
      return switch (type) {
         case GAG -> settings.gagUses();
         case BLINDFOLD -> settings.blindfoldUses();
         default -> 0;
      };
   }

   public void writeFingerprintData(ItemStack item, String data) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.fingerprintDataKey, PersistentDataType.STRING, data);
         meta.displayName(
            Component.text("Scheda impronte", NamedTextColor.GRAY)
               .decoration(TextDecoration.BOLD, false)
               .decoration(TextDecoration.ITALIC, false)
         );
         meta.lore(
            List.of(
               (TextComponent)Component.text("Traccia forense registrata.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
               (TextComponent)Component.text(data, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
            )
         );
         item.setItemMeta(meta);
      }
   }

   public String getFingerprintData(ItemStack item) {
      return item != null && item.hasItemMeta()
         ? (String)item.getItemMeta().getPersistentDataContainer().get(this.fingerprintDataKey, PersistentDataType.STRING)
         : null;
   }

   private List<Component> buildLore(UtilityItemType type, String... extra) {
      List<Component> lore = new ArrayList<>();

      for (String line : type.getLore()) {
         lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      }

      for (String line : extra) {
         lore.add(Component.text(line, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
      }

      return lore;
   }
}
