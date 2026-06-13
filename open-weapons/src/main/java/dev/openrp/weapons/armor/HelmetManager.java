package dev.openrp.weapons.armor;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.HelmetDefinition;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

public class HelmetManager {
   private final Map<String, HelmetDefinition> helmets = new LinkedHashMap<>();
   private final NamespacedKey helmetKey;
   private final NamespacedKey helmetDurabilityKey;
   private final CorePlugin core;

   public HelmetManager(CorePlugin core) {
      this.core = core;
      this.helmetKey = new NamespacedKey(core, "helmet_id");
      this.helmetDurabilityKey = new NamespacedKey(core, "helmet_durability");
      this.registerDefaults();
   }

   private void registerDefaults() {
      this.helmets.clear();
      this.helmets.putAll(defaultDefinitions());
   }

   public void load(File armorFile) {
      Map<String, HelmetDefinition> defaults = defaultDefinitions();
      if (armorFile == null || !armorFile.exists()) {
         this.helmets.clear();
         this.helmets.putAll(defaults);
         return;
      }

      YamlConfiguration config = YamlConfiguration.loadConfiguration(armorFile);
      ConfigurationSection root = config.getConfigurationSection("helmets");
      if (root == null) {
         this.helmets.clear();
         this.helmets.putAll(defaults);
         return;
      }

      Map<String, HelmetDefinition> loaded = new LinkedHashMap<>();
      for (Map.Entry<String, HelmetDefinition> entry : defaults.entrySet()) {
         ConfigurationSection section = root.getConfigurationSection(entry.getKey());
         loaded.put(entry.getKey(), readHelmet(entry.getKey(), section, entry.getValue()));
      }
      for (String id : root.getKeys(false)) {
         String normalizedId = id.toLowerCase(Locale.ROOT);
         if (loaded.containsKey(normalizedId)) {
            continue;
         }
         ConfigurationSection section = root.getConfigurationSection(id);
         if (section != null) {
            loaded.put(normalizedId, readHelmet(normalizedId, section, null));
         }
      }

      if (loaded.isEmpty()) {
         loaded.putAll(defaults);
      }
      this.helmets.clear();
      this.helmets.putAll(loaded);
      this.core.getLogger().info("[OpenWeapons] Caricate " + this.helmets.size() + " definizione/i casco da armor.yml.");
   }

   private Map<String, HelmetDefinition> defaultDefinitions() {
      Map<String, HelmetDefinition> defaults = new LinkedHashMap<>();
      defaults.put("ballistic_helmet", new HelmetDefinition("ballistic_helmet", "Casco balistico", 12020,
         0.15, true, false, 30, 0x323C32));
      defaults.put("riot_helmet", new HelmetDefinition("riot_helmet", "Casco antisommossa", 12021,
         0.0, false, true, 0, 0x1E1E1E));
      defaults.put("sf_helmet", new HelmetDefinition("sf_helmet", "Casco forze speciali", 12022,
         0.25, true, false, 50, 0x3C372D));
      return defaults;
   }

   private HelmetDefinition readHelmet(String id, ConfigurationSection section, HelmetDefinition fallback) {
      String displayName = fallback == null ? id : fallback.getDisplayName();
      int customModelData = fallback == null ? 0 : fallback.getCustomModelData();
      double damageReduction = fallback == null ? 0.0D : fallback.getDamageReduction();
      boolean negatesHeadshot = fallback != null && fallback.negatesHeadshot();
      boolean preventsMeleeStun = fallback != null && fallback.preventsMeleeStun();
      int maxDurability = fallback == null ? 0 : fallback.getMaxDurability();
      int colorRgb = fallback == null ? -1 : fallback.getColorRgb();

      if (section != null) {
         displayName = section.getString("display-name", displayName);
         customModelData = section.getInt("custom-model-data", customModelData);
         damageReduction = section.getDouble("damage-reduction", damageReduction);
         negatesHeadshot = section.getBoolean("negates-headshot", negatesHeadshot);
         preventsMeleeStun = section.getBoolean("prevents-melee-stun", preventsMeleeStun);
         maxDurability = section.getInt("max-durability", maxDurability);
         colorRgb = parseColor(section.getString("color-rgb", section.getString("color")), colorRgb);
      }

      return new HelmetDefinition(
         id,
         displayName == null || displayName.isBlank() ? id : displayName,
         Math.max(0, customModelData),
         clamp(damageReduction, 0.0D, 0.90D),
         negatesHeadshot,
         preventsMeleeStun,
         Math.max(0, maxDurability),
         colorRgb
      );
   }

   private int parseColor(String rawValue, int fallback) {
      if (rawValue == null || rawValue.isBlank()) {
         return fallback;
      }
      String value = rawValue.trim();
      try {
         if (value.startsWith("#")) {
            return Integer.parseInt(value.substring(1), 16) & 0xFFFFFF;
         }
         if (value.contains(",")) {
            String[] parts = value.split(",");
            if (parts.length == 3) {
               int red = Integer.parseInt(parts[0].trim());
               int green = Integer.parseInt(parts[1].trim());
               int blue = Integer.parseInt(parts[2].trim());
               return (clampColor(red) << 16) | (clampColor(green) << 8) | clampColor(blue);
            }
         }
         return Integer.decode(value) & 0xFFFFFF;
      } catch (NumberFormatException ex) {
         this.core.getLogger().warning("[OpenWeapons] Colore casco non valido '" + rawValue + "', using fallback.");
         return fallback;
      }
   }

   private int clampColor(int value) {
      return Math.max(0, Math.min(255, value));
   }

   private double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public HelmetDefinition getHelmet(String id) {
      return this.helmets.get(id);
   }

   public HelmetDefinition getHelmet(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         String id = (String)item.getItemMeta().getPersistentDataContainer().get(this.helmetKey, PersistentDataType.STRING);
         return id == null ? null : this.getHelmet(id);
      } else {
         return null;
      }
   }

   public List<HelmetDefinition> getAll() {
      return new ArrayList<>(this.helmets.values());
   }

   public int getDurability(ItemStack item) {
      return item != null && item.hasItemMeta()
         ? (Integer)item.getItemMeta().getPersistentDataContainer().getOrDefault(this.helmetDurabilityKey, PersistentDataType.INTEGER, -1)
         : 0;
   }

   public void setDurability(ItemStack item, int durability) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.helmetDurabilityKey, PersistentDataType.INTEGER, durability);
         HelmetDefinition def = this.getHelmet(item);
         if (def != null) {
            meta.lore(this.buildLore(def, durability));
         }

         item.setItemMeta(meta);
      }
   }

   public ItemStack createItemStack(String helmetId) {
      HelmetDefinition def = this.getHelmet(helmetId);
      if (def == null) {
         return null;
      }

      ItemStack item = new ItemStack(Material.LEATHER_HELMET);
      LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
      if (meta != null) {
         meta.displayName(
            Component.text(def.getDisplayName(), NamedTextColor.GRAY)
               .decoration(TextDecoration.BOLD, false)
               .decoration(TextDecoration.ITALIC, false)
         );
         if (def.getCustomModelData() > 0) {
            meta.setCustomModelData(def.getCustomModelData());
         }
         meta.setColor(resolveHelmetColor(def));

         meta.getPersistentDataContainer().set(this.helmetKey, PersistentDataType.STRING, helmetId);
         if (def.getMaxDurability() > 0) {
            meta.getPersistentDataContainer().set(this.helmetDurabilityKey, PersistentDataType.INTEGER, def.getMaxDurability());
         }

         meta.lore(this.buildLore(def, def.getMaxDurability()));
         item.setItemMeta(meta);
      }

      return item;
   }

   private Color resolveHelmetColor(HelmetDefinition def) {
      int rgb = def.getColorRgb();
      if (rgb >= 0) {
         return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
      }
      return switch (def.getId()) {
         case "ballistic_helmet" -> Color.fromRGB(50, 60, 50);
         case "riot_helmet" -> Color.fromRGB(30, 30, 30);
         case "sf_helmet" -> Color.fromRGB(60, 55, 45);
         default -> Color.fromRGB(45, 45, 45);
      };
   }

   private List<Component> buildLore(HelmetDefinition def, int currentDurability) {
      List<Component> lore = new ArrayList<>();
      lore.add(Component.text(""));
      if (def.getDamageReduction() > 0.0) {
         lore.add(
            ((TextComponent)Component.text("Protezione balistica: ", NamedTextColor.GRAY)
                  .append(Component.text(String.format("%.0f%%", def.getDamageReduction() * 100.0), NamedTextColor.GREEN)))
               .decoration(TextDecoration.ITALIC, false)
         );
      } else {
         lore.add(
            ((TextComponent)Component.text("Protezione balistica: ", NamedTextColor.GRAY).append(Component.text("Nessuna", NamedTextColor.RED)))
               .decoration(TextDecoration.ITALIC, false)
         );
      }

      if (def.negatesHeadshot()) {
         lore.add(Component.text("✦ Annulla il bonus colpo alla testa", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
      }

      if (def.preventsMeleeStun()) {
         lore.add(Component.text("✦ Previene lo stordimento melee", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("Protegge solo dal corpo a corpo", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
      }

      if (def.getMaxDurability() > 0) {
         lore.add(Component.text(""));
         int maxDur = def.getMaxDurability();
         int bars = 20;
         int filled = maxDur > 0 ? (int)Math.ceil((double)currentDurability / maxDur * bars) : bars;
         filled = Math.max(0, Math.min(bars, filled));
         StringBuilder durBar = new StringBuilder();

         for (int i = 0; i < bars; i++) {
            durBar.append(i < filled ? "█" : "░");
         }

         NamedTextColor durColor = filled > 10 ? NamedTextColor.GREEN : (filled > 5 ? NamedTextColor.YELLOW : NamedTextColor.RED);
         lore.add(
            ((TextComponent)((TextComponent)Component.text("Durabilita': ", NamedTextColor.GRAY).append(Component.text(durBar.toString(), durColor)))
                  .append(Component.text(" " + currentDurability + "/" + maxDur, NamedTextColor.GRAY)))
               .decoration(TextDecoration.ITALIC, false)
         );
      }

      return lore;
   }

   public NamespacedKey getHelmetKey() {
      return this.helmetKey;
   }

   public NamespacedKey getHelmetDurabilityKey() {
      return this.helmetDurabilityKey;
   }
}
