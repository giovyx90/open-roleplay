package dev.openrp.weapons.shield;

import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.util.JumpRestrictionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;

public class ShieldListener implements Listener {
   private final WeaponsModule module;

   public ShieldListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
   public void onProjectileDamage(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Player target && event.getDamager() instanceof Projectile projectile) {
         ShieldManager var7 = this.module.getShieldManager();
         Location source = this.projectileSourceLocation(projectile);
         if (var7.isRiotShieldBlocking(target, source)) {
            this.bypassVanillaShieldBlocking(event);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
   public void onHandSwap(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      ShieldManager sm = this.module.getShieldManager();
      ItemStack toOffHand = event.getOffHandItem();
      ItemStack currentMainHand = player.getInventory().getItemInMainHand();
      ItemStack currentOffHand = player.getInventory().getItemInOffHand();
      if (sm.isShield(toOffHand) || sm.isShield(currentMainHand) || sm.isShield(currentOffHand)) {
         event.setCancelled(true);
         this.denyOffhandShield(player);
         this.scheduleShieldStateUpdate(player);
      } else {
         this.scheduleShieldStateUpdate(player);
      }
   }

   @EventHandler
   public void onItemSwitch(PlayerItemHeldEvent event) {
      this.scheduleShieldStateUpdate(event.getPlayer());
   }

   @EventHandler
   public void onDropItem(PlayerDropItemEvent event) {
      this.scheduleShieldStateUpdate(event.getPlayer());
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         if (this.shouldCancelOffhandShieldClick(event, player)) {
            event.setCancelled(true);
            this.denyOffhandShield(player);
         }
         this.scheduleShieldStateUpdate(player);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         ShieldManager sm = this.module.getShieldManager();
         boolean putsShieldInOffhand = event.getNewItems().entrySet().stream()
            .anyMatch(entry -> this.isOffhandRawSlot(event.getView(), entry.getKey(), player) && sm.isShield(entry.getValue()));
         if (putsShieldInOffhand) {
            event.setCancelled(true);
            this.denyOffhandShield(player);
         }
         this.scheduleShieldStateUpdate(player);
      }
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      this.scheduleShieldStateUpdate(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      JumpRestrictionManager.release(event.getPlayer(), JumpRestrictionManager.REASON_SHIELD);
   }

   public void updateJumpRestriction(Player player) {
      if (player == null || !player.isOnline()) {
         return;
      }
      ShieldManager sm = this.module.getShieldManager();
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      boolean holdingShield = sm.isShield(mainHand) || sm.isShield(offHand);
      if (holdingShield) {
         JumpRestrictionManager.restrict(player, JumpRestrictionManager.REASON_SHIELD);
      } else {
         JumpRestrictionManager.release(player, JumpRestrictionManager.REASON_SHIELD);
      }
   }

   private void scheduleShieldStateUpdate(Player player) {
      Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
         this.sanitizeShieldOffhand(player);
         this.updateJumpRestriction(player);
      }, 1L);
   }

   private void sanitizeShieldOffhand(Player player) {
      if (player == null || !player.isOnline()) {
         return;
      }
      ShieldManager sm = this.module.getShieldManager();
      ItemStack offHand = player.getInventory().getItemInOffHand();
      if (!sm.isShield(offHand)) {
         return;
      }

      player.getInventory().setItemInOffHand(null);
      Map<Integer, ItemStack> leftovers = player.getInventory().addItem(offHand);
      leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
      this.denyOffhandShield(player);
      player.updateInventory();
   }

   private boolean shouldCancelOffhandShieldClick(InventoryClickEvent event, Player player) {
      ShieldManager sm = this.module.getShieldManager();
      if (event.getClick() == ClickType.SWAP_OFFHAND) {
         return sm.isShield(event.getCurrentItem()) || sm.isShield(player.getInventory().getItemInOffHand());
      }
      if (!this.isOffhandRawSlot(event.getView(), event.getRawSlot(), player)) {
         return false;
      }
      if (event.getClick() == ClickType.NUMBER_KEY) {
         int hotbarButton = event.getHotbarButton();
         ItemStack hotbarItem = hotbarButton >= 0 && hotbarButton <= 8
            ? player.getInventory().getItem(hotbarButton)
            : null;
         return sm.isShield(hotbarItem);
      }
      return sm.isShield(event.getCursor());
   }

   private boolean isOffhandRawSlot(org.bukkit.inventory.InventoryView view, int rawSlot, Player player) {
      if (rawSlot < 0 || rawSlot >= view.countSlots()) {
         return false;
      }
      Inventory inventory = view.getInventory(rawSlot);
      return inventory != null && inventory.equals(player.getInventory()) && view.convertSlot(rawSlot) == 40;
   }

   private void denyOffhandShield(Player player) {
      player.sendActionBar(Component.text("Shields can only be held in main hand!", NamedTextColor.RED));
   }

   private Location projectileSourceLocation(Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (shooter instanceof Entity entity) {
         return entity.getLocation();
      } else if (shooter instanceof BlockProjectileSource blockSource) {
         Block block = blockSource.getBlock();
         return block.getLocation().add(0.5, 0.5, 0.5);
      } else {
         return projectile.getLocation();
      }
   }

   private void bypassVanillaShieldBlocking(EntityDamageByEntityEvent event) {
      DamageModifier blocking = DamageModifier.BLOCKING;
      if (event.isApplicable(blocking) && event.getDamage(blocking) < 0.0) {
         event.setDamage(blocking, 0.0);
      }

      event.setCancelled(false);
   }
}
