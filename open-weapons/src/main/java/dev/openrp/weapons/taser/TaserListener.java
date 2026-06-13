package dev.openrp.weapons.taser;

import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.util.JumpRestrictionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class TaserListener implements Listener {
   private final WeaponsModule module;
   private final Map<UUID, Long> lastTaserTime = new HashMap<>();
   private final Map<UUID, Long> taseredPlayers = new HashMap<>();
   private static final long TASER_COOLDOWN_MS = 5000L;
   private static final long TASER_EFFECT_DURATION_TICKS = 100L;
   private static final double TASER_RANGE = 15.0;

   public TaserListener(WeaponsModule module) {
      this.module = module;
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (!this.module.getHandcuffManager().isRestrained(player)) {
         ItemStack item = event.getItem();
         WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(item);
         if (weapon != null && weapon.getCategory() == WeaponCategory.TASER) {
            event.setCancelled(true);
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
               long now = System.currentTimeMillis();
               Long lastUse = this.lastTaserTime.get(player.getUniqueId());
               if (lastUse != null && now - lastUse < 5000L) {
                  long remaining = (5000L - (now - lastUse)) / 1000L;
                  player.sendActionBar(Component.text("Taser in ricarica... " + remaining + "s", NamedTextColor.RED));
               } else {
                  this.lastTaserTime.put(player.getUniqueId(), now);
                  this.fireTaser(player, weapon);
               }
            }
         }
      }
   }

   private void fireTaser(Player shooter, WeaponDefinition weapon) {
      Location eyeLoc = shooter.getEyeLocation();
      Vector direction = eyeLoc.getDirection();
      shooter.getWorld().playSound(eyeLoc, Sound.ENTITY_BEE_STING, 1.5F, 0.5F);
      RayTraceResult result = shooter.getWorld().rayTraceEntities(eyeLoc, direction, 15.0, entity -> entity instanceof LivingEntity && !entity.equals(shooter));
      Location hitLoc;
      if (result != null && result.getHitEntity() instanceof LivingEntity target) {
         hitLoc = result.getHitPosition().toLocation(shooter.getWorld());
         target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false, true));
         target.damage(1.0, shooter);
         Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> {
            if (!target.isDead() && target.isValid()) {
               target.setVelocity(new Vector(0, 0, 0));
            }
         }, 1L);
         shooter.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 15, 0.2, 0.3, 0.2, 0.05);
         if (target instanceof Player targetPlayer) {
            String taserRestriction = "taser:" + System.nanoTime();
            JumpRestrictionManager.restrict(targetPlayer, taserRestriction);
            Bukkit.getScheduler().runTaskLater(this.module.getCore(), () -> JumpRestrictionManager.release(targetPlayer, taserRestriction), 100L);
            targetPlayer.sendActionBar(Component.text("⚡ Sei stato colpito dal taser! Non puoi correre o saltare!", NamedTextColor.YELLOW));
            this.taseredPlayers.put(targetPlayer.getUniqueId(), System.currentTimeMillis() + 5000L);
         }

         shooter.sendActionBar(Component.text("⚡ Taser a segno!", NamedTextColor.GREEN));
         shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
      } else {
         hitLoc = eyeLoc.clone().add(direction.clone().multiply(15.0));
         shooter.sendActionBar(Component.text("Taser mancato!", NamedTextColor.RED));
      }

      double distance = eyeLoc.distance(hitLoc);

      for (double d = 0.5; d < distance; d += 0.3) {
         Location point = eyeLoc.clone().add(direction.clone().multiply(d));
         DustOptions dust = new DustOptions(Color.fromRGB(255, 255, 0), 0.3F);
         shooter.getWorld().spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, dust);
      }
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      Long stunEnd = this.taseredPlayers.get(player.getUniqueId());
      if (stunEnd != null) {
         if (System.currentTimeMillis() > stunEnd) {
            this.taseredPlayers.remove(player.getUniqueId());
         } else {
            if (player.isSprinting()) {
               player.setSprinting(false);
            }
         }
      }
   }
}
