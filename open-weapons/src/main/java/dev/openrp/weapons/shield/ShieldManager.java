package dev.openrp.weapons.shield;

import it.meridian.core.CorePlugin;
import dev.openrp.weapons.model.AmmoDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class ShieldManager {
   private final NamespacedKey shieldKey;
   private final NamespacedKey shieldDurabilityKey;
   private final CorePlugin core;
   private final Map<UUID, Long> shieldCooldowns = new ConcurrentHashMap<>();
   public static final String RIOT_SHIELD_ID = "riot_shield";
   public static final String BALLISTIC_SHIELD_ID = "ballistic_shield";
   public static final int BALLISTIC_SHIELD_MAX_DURABILITY = 80;
   private static final int SHIELD_MODEL_DATA = 1;
   private static final long BALLISTIC_SHIELD_COOLDOWN_MILLIS = 10000L;
   private static final double BALLISTIC_SHIELD_BREAK_KNOCKBACK = 0.75;
   private static final double BALLISTIC_SHIELD_BREAK_LIFT = 0.18;

   public ShieldManager(CorePlugin core) {
      this.core = core;
      this.shieldKey = new NamespacedKey(core, "shield_id");
      this.shieldDurabilityKey = new NamespacedKey(core, "shield_durability");
   }

   public String getShieldId(ItemStack item) {
      return item != null && item.hasItemMeta() ? (String)item.getItemMeta().getPersistentDataContainer().get(this.shieldKey, PersistentDataType.STRING) : null;
   }

   public boolean isRiotShield(ItemStack item) {
      return "riot_shield".equals(this.getShieldId(item));
   }

   public boolean isBallisticShield(ItemStack item) {
      return "ballistic_shield".equals(this.getShieldId(item));
   }

   public boolean isShield(ItemStack item) {
      return this.getShieldId(item) != null;
   }

   public boolean isRiotShieldBlocking(Player player, Location source) {
      if (player != null && player.isBlocking()) {
         ItemStack mainHand = player.getInventory().getItemInMainHand();
         ItemStack offHand = player.getInventory().getItemInOffHand();
         return (this.isRiotShield(mainHand) || this.isRiotShield(offHand)) && this.isSourceInFront(player, source);
      } else {
         return false;
      }
   }

   public boolean isBallisticShieldBlocking(Player player, Location source) {
      if (player != null && player.isBlocking()) {
         ItemStack mainHand = player.getInventory().getItemInMainHand();
         if (!this.isBallisticShield(mainHand)) {
            return false;
         }

         if (this.isOnCooldown(player.getUniqueId())) {
            return false;
         }

         if (this.getDurability(mainHand) <= 0) {
            this.setDurability(mainHand, 80);
            player.sendActionBar(Component.text("Scudo balistico pronto.", NamedTextColor.GREEN));
         }

         return this.isBallisticShield(mainHand) && this.getDurability(mainHand) > 0 && this.isSourceInFront(player, source);
      } else {
         return false;
      }
   }

   private boolean isSourceInFront(Player player, Location source) {
      if (source != null && source.getWorld() != null && source.getWorld().equals(player.getWorld())) {
         Vector facing = player.getEyeLocation().getDirection();
         facing.setY(0);
         if (facing.lengthSquared() == 0.0) {
            return true;
         }

         facing.normalize();
         Vector incoming = source.toVector().subtract(player.getLocation().toVector());
         incoming.setY(0);
         if (incoming.lengthSquared() == 0.0) {
            return true;
         }

         incoming.normalize();
         return facing.dot(incoming) > 0.2;
      } else {
         return false;
      }
   }

   public int getDurability(ItemStack item) {
      return item != null && item.hasItemMeta()
         ? (Integer)item.getItemMeta().getPersistentDataContainer().getOrDefault(this.shieldDurabilityKey, PersistentDataType.INTEGER, 80)
         : 0;
   }

   public void setDurability(ItemStack item, int durability) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         meta.getPersistentDataContainer().set(this.shieldDurabilityKey, PersistentDataType.INTEGER, durability);
         if (this.isBallisticShield(item)) {
            meta.lore(this.buildBallisticLore(durability));
         }

         item.setItemMeta(meta);
      }
   }

   public int getShieldDurabilityDamage(String ammoType) {
      if (ammoType == null) {
         return 1;
      }

      return switch (ammoType) {
         case "50bmg" -> 999;
         case "338lapua" -> 8;
         case "762nato" -> 5;
         case "762x39" -> 4;
         case "556nato" -> 3;
         case "12gauge" -> 3;
         case "50ae" -> 5;
         case "500magnum" -> 5;
         case "357magnum" -> 2;
         default -> 1;
      };
   }

   public int getShieldDurabilityDamage(AmmoDefinition ammo) {
      return ammo != null ? Math.max(0, ammo.getShieldDurabilityDamage()) : 1;
   }

   public void startCooldown(UUID playerId) {
      this.shieldCooldowns.put(playerId, System.currentTimeMillis() + 10000L);
   }

   public void startCooldown(Player player) {
      this.startCooldown(player, null);
   }

   public void startCooldown(Player player, Location source) {
      UUID playerId = player.getUniqueId();
      this.startCooldown(playerId);
      this.knockBackFrom(player, source);
      Bukkit.getScheduler().runTaskLater(this.core, () -> {
         this.shieldCooldowns.remove(playerId);
         if (player.isOnline()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (this.isBallisticShield(mainHand) && this.getDurability(mainHand) <= 0) {
               this.setDurability(mainHand, 80);
               player.sendMessage(Component.text("Scudo balistico di nuovo pronto.", NamedTextColor.GREEN));
            }
         }
      }, 200L);
   }

   private void knockBackFrom(Player player, Location source) {
      Vector knockback;
      if (source != null && source.getWorld() != null && source.getWorld().equals(player.getWorld())) {
         knockback = player.getLocation().toVector().subtract(source.toVector());
         knockback.setY(0);
      } else {
         knockback = player.getEyeLocation().getDirection().multiply(-1);
         knockback.setY(0);
      }

      if (knockback.lengthSquared() < 0.001) {
         knockback = player.getEyeLocation().getDirection().multiply(-1);
         knockback.setY(0);
      }

      if (knockback.lengthSquared() < 0.001) {
         knockback = new Vector(0, 0, -1);
      }

      knockback.normalize().multiply(0.75).setY(0.18);
      player.setVelocity(knockback);
   }

   public boolean isOnCooldown(UUID playerId) {
      Long cooldownEnd = this.shieldCooldowns.get(playerId);
      if (cooldownEnd == null) {
         return false;
      } else if (System.currentTimeMillis() > cooldownEnd) {
         this.shieldCooldowns.remove(playerId);
         return false;
      } else {
         return true;
      }
   }

   public long getRemainingCooldownSeconds(UUID playerId) {
      Long cooldownEnd = this.shieldCooldowns.get(playerId);
      if (cooldownEnd == null) {
         return 0L;
      }

      long remaining = cooldownEnd - System.currentTimeMillis();
      return remaining > 0L ? remaining / 1000L : 0L;
   }

   public ItemStack createRiotShield() {
      ItemStack item = new ItemStack(Material.SHIELD);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(
            Component.text("Scudo antisommossa", NamedTextColor.GRAY)
               .decoration(TextDecoration.BOLD, false)
               .decoration(TextDecoration.ITALIC, false)
         );
         meta.setCustomModelData(SHIELD_MODEL_DATA);
         meta.getPersistentDataContainer().set(this.shieldKey, PersistentDataType.STRING, "riot_shield");
         List<Component> lore = new ArrayList<>();
         lore.add(Component.text(""));
         lore.add(Component.text("Blocca danni corpo a corpo", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("NON blocca i proiettili", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text(""));
         lore.add(Component.text("⚠ Impedisce di saltare mentre lo tieni", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("Solo mano principale", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         meta.lore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   public ItemStack createBallisticShield() {
      ItemStack item = new ItemStack(Material.SHIELD);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(
            Component.text("Scudo balistico", NamedTextColor.GRAY)
               .decoration(TextDecoration.BOLD, false)
               .decoration(TextDecoration.ITALIC, false)
         );
         meta.setCustomModelData(SHIELD_MODEL_DATA);
         meta.getPersistentDataContainer().set(this.shieldKey, PersistentDataType.STRING, "ballistic_shield");
         meta.getPersistentDataContainer().set(this.shieldDurabilityKey, PersistentDataType.INTEGER, 80);
         meta.lore(this.buildBallisticLore(80));
         item.setItemMeta(meta);
      }

      return item;
   }

   private List<Component> buildBallisticLore(int currentDurability) {
      List<Component> lore = new ArrayList<>();
      lore.add(Component.text(""));
      lore.add(Component.text("✦ Blocca danni da proiettile", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("⚠ Impedisce di saltare mentre lo tieni", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("Solo mano principale", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text(""));
      int maxDur = 80;
      int bars = 20;
      int filled = (int)Math.ceil((double)currentDurability / maxDur * bars);
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
      lore.add(Component.text(""));
      lore.add(Component.text("⚠ Il .50 BMG distrugge questo scudo", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
      return lore;
   }

   public NamespacedKey getShieldKey() {
      return this.shieldKey;
   }
}
