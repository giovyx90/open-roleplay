package dev.openrp.weapons.mechanics;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ChargedProjectiles;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

class PacketEventsWeaponAnimationHook implements WeaponAnimationSuppressor.AnimationPacketHook {
   private final WeaponsModule module;
   private final Map<Integer, Long> suppressEntityIds;
   private final Map<UUID, Long> aimingSwingDebounce = new ConcurrentHashMap<>();
   private final NamespacedKey aimingProxyKey;
   private Consumer<Player> aimingSwingHandler;
   private PacketListenerCommon packetListener;

   PacketEventsWeaponAnimationHook(WeaponsModule module, Map<Integer, Long> suppressEntityIds) {
      this.module = module;
      this.suppressEntityIds = suppressEntityIds;
      this.aimingProxyKey = new NamespacedKey(module.getCore(), "weapon_aiming_proxy");
   }

   @Override
   public void enable() {
      this.packetListener = new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
         @Override
         public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.ANIMATION) {
               return;
            }
            Player player = event.getPlayer();
            if (player == null || !PacketEventsWeaponAnimationHook.this.isFirearmView(player)) {
               return;
            }
            try {
               WrapperPlayClientAnimation wrapper = new WrapperPlayClientAnimation(event);
               if (wrapper.getHand() != InteractionHand.MAIN_HAND && wrapper.getHand() != InteractionHand.OFF_HAND) {
                  return;
               }
            } catch (Throwable ignored) {
               // Older/minor protocol variants can still be safely suppressed by packet type.
            }
            PacketEventsWeaponAnimationHook.this.suppressSwing(player);
            event.setCancelled(true);
            PacketEventsWeaponAnimationHook.this.dispatchAimingSwing(player);
         }

         @Override
         public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.ENTITY_ANIMATION) {
               return;
            }

            WrapperPlayServerEntityAnimation wrapper;
            try {
               wrapper = new WrapperPlayServerEntityAnimation(event);
            } catch (Throwable ignored) {
               return;
            }

            WrapperPlayServerEntityAnimation.EntityAnimationType type = wrapper.getType();
            if (type != WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                  && type != WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND) {
               return;
            }

            int entityId = wrapper.getEntityId();
            if (PacketEventsWeaponAnimationHook.this.isSuppressed(entityId)
                  || PacketEventsWeaponAnimationHook.this.isFirearmHolder(entityId)) {
               event.setCancelled(true);
            }
         }
      };
      PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
   }

   @Override
   public void disable() {
      if (this.packetListener != null && PacketEvents.getAPI() != null) {
         PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
      }

      this.packetListener = null;
      this.aimingSwingHandler = null;
      this.aimingSwingDebounce.clear();
   }

   @Override
   public void showAimingPose(Player player, boolean aiming) {
      AimingEquipment equipment = aiming ? this.createAimingEquipment(player) : this.createNormalEquipment(player);

      for (Player viewer : player.getWorld().getPlayers()) {
         if (viewer.equals(player) || viewer.canSee(player)) {
            AimingView view = viewer.equals(player) ? equipment.self() : equipment.viewers();
            viewer.sendEquipmentChange(player, EquipmentSlot.HAND, view.hand());
            viewer.sendEquipmentChange(player, EquipmentSlot.OFF_HAND, view.offHand());
         }
      }
   }

   @Override
   public void setAimingSwingHandler(Consumer<Player> aimingSwingHandler) {
      this.aimingSwingHandler = aimingSwingHandler;
   }

   private AimingEquipment createAimingEquipment(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      boolean proxyAim = this.isAimingProxy(mainHand);
      if (proxyAim) {
         ItemStack viewerHand = this.isFirearm(offHand) ? this.createAimingWeaponVisual(offHand) : offHand;
         return new AimingEquipment(
            new AimingView(mainHand, viewerHand),
            new AimingView(viewerHand, air())
         );
      }
      ItemStack visual = this.createAimingWeaponVisual(mainHand);
      AimingView visualView = new AimingView(visual, offHand);
      return new AimingEquipment(visualView, visualView);
   }

   private AimingEquipment createNormalEquipment(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      AimingView normalView = new AimingView(mainHand, offHand);
      return new AimingEquipment(normalView, normalView);
   }

   private static ItemStack air() {
      return new ItemStack(Material.AIR);
   }

   private void dispatchAimingSwing(Player player) {
      Consumer<Player> handler = this.aimingSwingHandler;
      if (handler == null) {
         return;
      }

      UUID uuid = player.getUniqueId();
      long now = System.currentTimeMillis();
      Long lastSwing = this.aimingSwingDebounce.get(uuid);
      if (lastSwing != null && now - lastSwing < 30L) {
         return;
      }
      this.aimingSwingDebounce.put(uuid, now);

      Bukkit.getScheduler().runTask(this.module.getCore(), () -> {
         Player online = Bukkit.getPlayer(uuid);
         if (online != null && online.isOnline()) {
            handler.accept(online);
         }
      });
   }

   private void suppressSwing(Player player) {
      this.suppressEntityIds.put(player.getEntityId(), System.currentTimeMillis() + 800L);
      if (this.isAimingView(player)) {
         this.scheduleAimingPoseRefresh(player);
      }
   }

   private void scheduleAimingPoseRefresh(Player player) {
      UUID uuid = player.getUniqueId();
      Bukkit.getScheduler().runTask(this.module.getCore(), () -> {
         Player online = Bukkit.getPlayer(uuid);
         if (online != null && online.isOnline() && this.isAimingView(online)) {
            this.showAimingPose(online, true);
         }
      });
   }

   private ItemStack createAimingWeaponVisual(ItemStack source) {
      if (source == null || source.getType().isAir()) {
         return new ItemStack(Material.AIR);
      }

      WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(source);
      ItemStack visual = source.clone();
      if (weapon == null) {
         return visual;
      }
      if (weapon.getMaterial() == Material.CROSSBOW && visual.getType() == Material.CROSSBOW) {
         visual.resetData(DataComponentTypes.CHARGED_PROJECTILES);
         visual.resetData(DataComponentTypes.CONSUMABLE);
         visual.setData(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectiles.chargedProjectiles()
            .addAll(List.of(new ItemStack(Material.ARROW)))
            .build());
         return visual;
      }
      if (visual.getType() == weapon.getMaterial()) {
         this.module.getWeaponRegistry().applyFirearmUseAnimation(visual, weapon);
      }
      return visual;
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

   private boolean isSuppressed(int entityId) {
      Long until = this.suppressEntityIds.get(entityId);
      if (until == null) {
         return false;
      } else if (System.currentTimeMillis() > until) {
         this.suppressEntityIds.remove(entityId);
         return false;
      } else {
         return true;
      }
   }

   private boolean isFirearmHolder(int entityId) {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (player.getEntityId() == entityId && this.isFirearmView(player)) {
            return true;
         }
      }

      return false;
   }

   private record AimingEquipment(AimingView self, AimingView viewers) {
   }

   private record AimingView(ItemStack hand, ItemStack offHand) {
   }
}
