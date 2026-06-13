package dev.openrp.weapons.mechanics;

import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class WeaponAnimationSuppressor implements Listener {
   private static final long SUPPRESS_MILLIS = 800L;
   private final WeaponsModule module;
   private final NamespacedKey aimingProxyKey;
   private final Map<Integer, Long> suppressEntityIds = new ConcurrentHashMap<>();
   private WeaponAnimationSuppressor.AnimationPacketHook protocolHook;

   public WeaponAnimationSuppressor(WeaponsModule module) {
      this.module = module;
      this.aimingProxyKey = new NamespacedKey(module.getCore(), "weapon_aiming_proxy");
   }

   public void enablePacketHook() {
      if (Bukkit.getPluginManager().getPlugin("packetevents") == null) {
         this.module.getCore().getLogger().info("[OpenWeapons] PacketEvents non trovato; uso la soppressione animazioni arma solo tramite Bukkit.");
      } else {
         try {
            this.protocolHook = new PacketEventsWeaponAnimationHook(this.module, this.suppressEntityIds);
            this.protocolHook.enable();
            this.module.getCore().getLogger().info("[OpenWeapons] Soppressione animazioni arma tramite PacketEvents abilitata.");
         } catch (Throwable throwable) {
            this.protocolHook = null;
            this.module
               .getCore()
               .getLogger()
               .warning("[OpenWeapons] Hook PacketEvents non riuscito; uso la soppressione animazioni arma solo tramite Bukkit: " + throwable.getMessage());
         }
      }
   }

   public void disablePacketHook() {
      if (this.protocolHook != null) {
         this.protocolHook.disable();
      }

      this.protocolHook = null;
      this.suppressEntityIds.clear();
   }

   public void setAimingSwingHandler(Consumer<Player> aimingSwingHandler) {
      if (this.protocolHook != null) {
         this.protocolHook.setAimingSwingHandler(aimingSwingHandler);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
   public void onPlayerAnimation(PlayerAnimationEvent event) {
      Player player = event.getPlayer();
      if (this.isFirearmView(player)) {
         this.suppressSwing(player);
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
   public void onPlayerArmSwing(PlayerArmSwingEvent event) {
      if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
         return;
      }

      Player player = event.getPlayer();
      if (this.isFirearmView(player)) {
         this.suppressSwing(player);
         event.setCancelled(true);
      }
   }

   public void suppress(Player player) {
      this.suppressEntityIds.put(player.getEntityId(), System.currentTimeMillis() + SUPPRESS_MILLIS);
   }

   public void showAimingPose(Player player, boolean aiming) {
      if (this.protocolHook != null) {
         this.protocolHook.showAimingPose(player, aiming);
      }
   }

   private void suppressSwing(Player player) {
      this.suppress(player);
      if (this.isAimingView(player)) {
         this.showAimingPose(player, true);
      }
   }

   private boolean isFirearm(ItemStack item) {
      WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(item);
      return weapon != null && weapon.getCategory() != WeaponCategory.MELEE && weapon.getCategory() != WeaponCategory.TASER;
   }

   private boolean isFirearmView(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      return this.isFirearm(mainHand) || this.isFirearm(offHand) || this.isAimingProxy(mainHand);
   }

   private boolean isAimingView(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      return this.isAimingProxy(mainHand) || player.isSneaking() && this.isFirearmView(player);
   }

   private boolean isAimingProxy(ItemStack item) {
      return item != null
         && !item.getType().isAir()
         && item.hasItemMeta()
         && item.getItemMeta().getPersistentDataContainer().has(this.aimingProxyKey, PersistentDataType.BYTE);
   }

   interface AnimationPacketHook {
      void enable();

      void disable();

      default void showAimingPose(Player player, boolean aiming) {
      }

      default void setAimingSwingHandler(Consumer<Player> aimingSwingHandler) {
      }
   }
}
