package dev.openrp.weapons.dispatch;

import it.meridian.core.CorePlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class DispatchGpsManager {
   private static final long DEFAULT_DURATION_TICKS = 2400L;
   private static final double DEFAULT_ARRIVAL_DISTANCE = 6.0;
   private static final String[] GPS_DIRECTIONS = new String[]{"Ahead", "Ahead left", "Left", "Behind left", "Behind", "Behind right", "Right", "Ahead right"};
   private final CorePlugin core;
   private final Map<UUID, DispatchGpsManager.GpsSession> activeGps = new ConcurrentHashMap<>();

   public DispatchGpsManager(CorePlugin core) {
      this.core = core;
   }

   public void activate(Player player, String label, Supplier<Location> targetSupplier) {
      this.activate(player, label, targetSupplier, DEFAULT_DURATION_TICKS, DEFAULT_ARRIVAL_DISTANCE);
   }

   public void activate(Player player, String label, Supplier<Location> targetSupplier, long durationTicks, double arrivalDistance) {
      this.activate(player, label, targetSupplier, durationTicks, arrivalDistance, true);
   }

   public void activate(Player player, String label, Supplier<Location> targetSupplier, long durationTicks, double arrivalDistance, boolean showCoordinates) {
      this.stop(player, false);
      BossBar bar = BossBar.bossBar(Component.empty(), 1.0F, Color.YELLOW, Overlay.PROGRESS);
      Location previousCompass = player.getCompassTarget() != null ? player.getCompassTarget().clone() : null;
      long expiresAt = System.currentTimeMillis() + Math.max(1L, durationTicks) * 50L;
      DispatchGpsManager.GpsSession session = new DispatchGpsManager.GpsSession(label, targetSupplier, bar, previousCompass, expiresAt, arrivalDistance, showCoordinates);
      this.activeGps.put(player.getUniqueId(), session);
      player.showBossBar(bar);
      session.task = Bukkit.getScheduler().runTaskTimer(this.core, () -> this.update(player), 0L, 5L);
      player.sendMessage(Component.text(label + " GPS activated.", NamedTextColor.GREEN));
   }

   public void stop(Player player, boolean notify) {
      DispatchGpsManager.GpsSession session = this.activeGps.remove(player.getUniqueId());
      if (session == null) {
         if (notify) {
            player.sendMessage(Component.text("No active dispatch GPS.", NamedTextColor.RED));
         }
      } else {
         if (session.task != null) {
            session.task.cancel();
         }

         player.hideBossBar(session.bar);
         if (session.previousCompassTarget != null) {
            player.setCompassTarget(session.previousCompassTarget);
         }

         if (notify) {
            player.sendMessage(Component.text("Dispatch GPS disabled.", NamedTextColor.GREEN));
         }
      }
   }

   public void cleanup() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         this.stop(player, false);
      }

      this.activeGps.clear();
   }

   private void update(Player player) {
      DispatchGpsManager.GpsSession session = this.activeGps.get(player.getUniqueId());
      if (session != null) {
         if (!player.isOnline()) {
            this.activeGps.remove(player.getUniqueId());
            if (session.task != null) {
               session.task.cancel();
            }
         } else if (System.currentTimeMillis() >= session.expiresAtMillis) {
            this.stop(player, false);
            player.sendMessage(Component.text(session.label + " GPS expired.", NamedTextColor.YELLOW));
         } else {
            Location target = this.resolveTarget(session);
            if (target != null && target.getWorld() != null) {
               target = target.clone();
               if (!player.getWorld().equals(target.getWorld())) {
                  session.bar.name(Component.text(session.label + " GPS signal unavailable in this world. Target: " + target.getWorld().getName(), NamedTextColor.RED));
               } else {
                  Location playerLocation = player.getLocation();
                  double distance = playerLocation.distance(target);
                  if (distance <= session.arrivalDistance) {
                     this.stop(player, false);
                     player.sendMessage(Component.text(session.label + " GPS target reached.", NamedTextColor.GREEN));
                  } else {
                     player.setCompassTarget(target);
                     session.bar.progress(Math.max(0.05F, Math.min(1.0F, (float)(distance / 120.0))));
                     session.bar.name(Component.text(this.formatGpsText(playerLocation, target, session.label, distance, session.showCoordinates), NamedTextColor.YELLOW));
                  }
               }
            } else {
               session.bar.name(Component.text(session.label + " GPS signal unavailable. Waiting for target...", NamedTextColor.RED));
            }
         }
      }
   }

   private Location resolveTarget(DispatchGpsManager.GpsSession session) {
      try {
         return session.targetSupplier.get();
      } catch (RuntimeException ignored) {
         return null;
      }
   }

   String formatGpsText(Location from, Location to, String label, double distance) {
      return formatGpsText(from, to, label, distance, true);
   }

   String formatGpsText(Location from, Location to, String label, double distance, boolean showCoordinates) {
      String text = directionLabel(from.getYaw(), to.getX() - from.getX(), to.getZ() - from.getZ())
         + " | "
         + label
         + " | "
         + Math.round(distance)
         + "m";
      if (!showCoordinates) {
         return text;
      }
      return directionLabel(from.getYaw(), to.getX() - from.getX(), to.getZ() - from.getZ())
         + " | "
         + label
         + " | "
         + Math.round(distance)
         + "m | "
         + this.formatCoordinates(to);
   }

   static String directionLabel(float yaw, double dx, double dz) {
      return GPS_DIRECTIONS[directionIndex(yaw, dx, dz)];
   }

   static int directionIndex(float yaw, double dx, double dz) {
      double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
      double relative = normalizeDegreesStatic(targetYaw - yaw);
      return (int)Math.floor((relative + 22.5) / 45.0) & 7;
   }

   private String formatCoordinates(Location location) {
      return location.getWorld().getName() + " "
         + Math.round(location.getX()) + " "
         + Math.round(location.getY()) + " "
         + Math.round(location.getZ());
   }

   private int getArrowIndex(Location from, Location to) {
      return directionIndex(from.getYaw(), to.getX() - from.getX(), to.getZ() - from.getZ());
   }

   private static double normalizeDegreesStatic(double degrees) {
      degrees %= 360.0;
      if (degrees < 0.0) {
         degrees += 360.0;
      }

      return degrees;
   }

   private static class GpsSession {
      private final String label;
      private final Supplier<Location> targetSupplier;
      private final BossBar bar;
      private final Location previousCompassTarget;
      private final long expiresAtMillis;
      private final double arrivalDistance;
      private final boolean showCoordinates;
      private BukkitTask task;

      private GpsSession(
         String label,
         Supplier<Location> targetSupplier,
         BossBar bar,
         Location previousCompassTarget,
         long expiresAtMillis,
         double arrivalDistance,
         boolean showCoordinates
      ) {
         this.label = label;
         this.targetSupplier = targetSupplier;
         this.bar = bar;
         this.previousCompassTarget = previousCompassTarget;
         this.expiresAtMillis = expiresAtMillis;
         this.arrivalDistance = arrivalDistance;
         this.showCoordinates = showCoordinates;
      }
   }
}
