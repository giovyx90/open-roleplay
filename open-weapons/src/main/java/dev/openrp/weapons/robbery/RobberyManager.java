package dev.openrp.weapons.robbery;

import dev.openrp.weapons.bridge.staff.StaffBoardMetadata;
import dev.openrp.weapons.bridge.staff.StaffBoardCategory;
import dev.openrp.weapons.bridge.staff.StaffBoardLogEvent;
import dev.openrp.weapons.bridge.staff.StaffBoardSensitivity;
import dev.openrp.weapons.bridge.staff.StaffBoardSeverity;
import dev.openrp.weapons.util.OpenPermissions;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.utility.StatusTextDisplays;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

public class RobberyManager {
   private final WeaponsModule module;
   private final Map<UUID, RobberySession> activeRobberies = new HashMap<>();
   private final Map<UUID, Map<LocalDate, Integer>> dailyRobberies = new HashMap<>();
   private final Map<UUID, UUID> pendingSlogs = new HashMap<>();
   private final Map<UUID, TextDisplay> abbattibileTags = new HashMap<>();
   private static final int MAX_DAILY_ROBBERIES = 2;
   private static final int ROBBERY_DURATION_TICKS = 6000;
   private static final double FLEE_DISTANCE = 10.0;
   private static final int ABBATTIBILE_SECONDS = 300;

   public RobberyManager(WeaponsModule module) {
      this.module = module;
   }

   public boolean canRobToday(Player player) {
      if (canBypassDailyLimit(player)) {
         return true;
      }

      LocalDate today = LocalDate.now();
      Map<LocalDate, Integer> playerHistory = this.dailyRobberies.getOrDefault(player.getUniqueId(), new HashMap<>());
      int count = playerHistory.getOrDefault(today, 0);
      return count < 2;
   }

   public void incrementDailyRobberies(Player player) {
      if (!canBypassDailyLimit(player)) {
         LocalDate today = LocalDate.now();
         Map<LocalDate, Integer> playerHistory = this.dailyRobberies.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
         playerHistory.put(today, playerHistory.getOrDefault(today, 0) + 1);
      }
   }

   private boolean canBypassDailyLimit(Player player) {
      return OpenPermissions.hasAny(player,
            OpenPermissions.Robbery.ADMIN,
            OpenPermissions.Weapons.ADMIN,
            OpenPermissions.Staff.ADMIN,
            OpenPermissions.Test.DEBUG);
   }

   public boolean isBeingRobbed(UUID victimUuid) {
      return this.activeRobberies.containsKey(victimUuid);
   }

   public boolean hasActiveRobbery(UUID robberUuid) {
      for (RobberySession session : this.activeRobberies.values()) {
         if (session.getRobberUuid().equals(robberUuid)) {
            return true;
         }
      }

      return false;
   }

   public void startRobbery(final Player robber, final Player victim) {
      final RobberySession session = new RobberySession(robber.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis(), victim.getLocation().clone());
      double tagOffset = module.getUtilitySettings().statusTagYOffset();
      TextDisplay display = StatusTextDisplays.spawn(victim,
            Component.text("☠ SOTTO RAPINA ☠", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}),
            tagOffset);
      session.setTextDisplay(display);
      session.setTask((new BukkitRunnable() {
         public void run() {
            if (RobberyManager.this.activeRobberies.containsKey(victim.getUniqueId())) {
               RobberyManager.this.endRobbery(victim.getUniqueId());
               if (robber.isOnline()) {
                  robber.sendMessage(Component.text("Sessione rapina con " + victim.getName() + " terminata.", NamedTextColor.YELLOW));
               }

               if (victim.isOnline()) {
                  victim.sendMessage(Component.text("La tua sessione rapina e' terminata.", NamedTextColor.GREEN));
               }
            }
         }
      }).runTaskLater(this.module.getCore(), 6000L));
      session.setDistanceTask((new BukkitRunnable() {
         public void run() {
            if (!RobberyManager.this.activeRobberies.containsKey(victim.getUniqueId())) {
               this.cancel();
            } else if (!victim.isOnline() || !robber.isOnline()) {
               this.cancel();
            } else if (session.isMarkedAbbattibile()) {
               this.cancel();
            } else if (!victim.getWorld().equals(session.getStartLocation().getWorld())) {
               RobberyManager.this.applyFleeAbbattibile(victim, robber, session);
               this.cancel();
            } else if (!robber.getWorld().equals(session.getStartLocation().getWorld())) {
               RobberyManager.this.endRobbery(victim.getUniqueId());
               robber.sendMessage(Component.text("Hai lasciato l'area rapina. Rapina annullata.", NamedTextColor.YELLOW));
               victim.sendMessage(Component.text("Il rapinatore si e' allontanato. Rapina annullata.", NamedTextColor.GREEN));
               this.cancel();
            } else {
               double vDist = victim.getLocation().distance(session.getStartLocation());
               double rDist = robber.getLocation().distance(session.getStartLocation());
               StatusTextDisplays.follow(display, victim, tagOffset);
               if (vDist > 10.0) {
                  RobberyManager.this.applyFleeAbbattibile(victim, robber, session);
                  this.cancel();
               } else if (rDist > 10.0) {
                  RobberyManager.this.endRobbery(victim.getUniqueId());
                  robber.sendMessage(Component.text("Hai lasciato l'area rapina. Rapina annullata.", NamedTextColor.YELLOW));
                  victim.sendMessage(Component.text("Il rapinatore si e' allontanato. Rapina annullata.", NamedTextColor.GREEN));
                  this.cancel();
               }
            }
         }
      }).runTaskTimer(this.module.getCore(), 20L, 20L));
      this.activeRobberies.put(victim.getUniqueId(), session);
      this.incrementDailyRobberies(robber);
      emitRobberyEvent("crime.robbery.started", robber, victim, session, "Rapina giocatore iniziata.",
              StaffBoardSeverity.WARNING);
   }

   private void applyFleeAbbattibile(final Player victim, Player robber, RobberySession session) {
      session.setMarkedAbbattibile(true);
      this.endRobbery(victim.getUniqueId());
      emitRobberyEvent("crime.robbery.failed", robber, victim, session, "La vittima e' fuggita dalla rapina.",
              StaffBoardSeverity.WARNING);
      robber.sendMessage(Component.text(victim.getName() + " e' fuggito dalla rapina! Ora e' Abbattibile per 5 minuti.", NamedTextColor.GREEN));
      victim.sendMessage(
         Component.text("Sei fuggito dalla rapina! Ora sei Abbattibile per 5 minuti.", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      double tagOffset = module.getUtilitySettings().statusTagYOffset();
      final TextDisplay td = StatusTextDisplays.spawn(victim,
            Component.text("☠ Abbattibile - 5:00 ☠", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD}),
            tagOffset);
      this.abbattibileTags.put(victim.getUniqueId(), td);
      int abbattibileTicks = ABBATTIBILE_SECONDS * 20;
      (new BukkitRunnable() {
            int ticks = 0;

            public void run() {
               if (this.ticks < abbattibileTicks && victim.isOnline()) {
                  if (!victim.isDead() && td.isValid()) {
                     int secondsLeft = (abbattibileTicks - this.ticks) / 20;
                     int mins = secondsLeft / 60;
                     int secs = secondsLeft % 60;
                     td.text(
                        Component.text(
                           "☠ Abbattibile - " + mins + ":" + String.format("%02d", secs) + " ☠",
                           NamedTextColor.DARK_RED,
                           new TextDecoration[]{TextDecoration.BOLD}
                        )
                     );
                     StatusTextDisplays.follow(td, victim, tagOffset);
                  }

                  this.ticks++;
               } else {
                  if (td.isValid()) {
                     td.remove();
                  }

                  RobberyManager.this.abbattibileTags.remove(victim.getUniqueId());
                  if (victim.isOnline()) {
                     victim.sendMessage(Component.text("Non sei piu' Abbattibile.", NamedTextColor.GREEN));
                  }

                  this.cancel();
               }
            }
         })
         .runTaskTimer(this.module.getCore(), 0L, 1L);
   }

   public void endRobbery(UUID victimUuid) {
      RobberySession session = this.activeRobberies.remove(victimUuid);
      if (session != null) {
         if (session.getTextDisplay() != null) {
            session.getTextDisplay().remove();
         }

         if (session.getTask() != null) {
            session.getTask().cancel();
         }

         if (session.getDistanceTask() != null) {
            session.getDistanceTask().cancel();
         }
      }
   }

   public RobberySession getSession(UUID victimUuid) {
      return this.activeRobberies.get(victimUuid);
   }

   /**
    * Ends every active robbery in which {@code robberUuid} is the robber. Mirrors the victim-side
    * cleanup so a robber quitting or dying does not leak the session, its status display and tasks.
    */
   public void endRobberiesByRobber(UUID robberUuid) {
      java.util.List<UUID> victims = new java.util.ArrayList<>();
      for (RobberySession session : this.activeRobberies.values()) {
         if (session.getRobberUuid().equals(robberUuid)) {
            victims.add(session.getVictimUuid());
         }
      }
      for (UUID victim : victims) {
         endRobbery(victim);
      }
   }

   /**
    * Drops day-count entries for any date other than today, and forgets players left with no
    * remaining counts, so {@link #dailyRobberies} cannot grow without bound over a long uptime.
    * Today's counts are preserved so the daily limit can't be reset by reconnecting.
    */
   public void pruneDailyRobberies() {
      LocalDate today = LocalDate.now();
      this.dailyRobberies.values().forEach(history -> history.keySet().removeIf(date -> !date.equals(today)));
      this.dailyRobberies.values().removeIf(Map::isEmpty);
   }

   /**
    * Cleans up all robbery state and removes every status display entity from the world. Intended to
    * be called on plugin disable; the scheduler cancels the backing tasks, but the spawned
    * {@link TextDisplay} entities are persistent and must be removed explicitly.
    */
   public void cleanup() {
      for (UUID victim : new java.util.ArrayList<>(this.activeRobberies.keySet())) {
         endRobbery(victim);
      }
      for (TextDisplay tag : this.abbattibileTags.values()) {
         if (tag != null && tag.isValid()) {
            tag.remove();
         }
      }
      this.abbattibileTags.clear();
      this.activeRobberies.clear();
      this.pendingSlogs.clear();
      this.dailyRobberies.clear();
   }

   public void markPendingSlog(UUID victimUuid, UUID robberUuid) {
      this.pendingSlogs.put(victimUuid, robberUuid);
   }

   public UUID getPendingSlogRobber(UUID victimUuid) {
      return this.pendingSlogs.remove(victimUuid);
   }

   public WeaponsModule getModule() {
      return this.module;
   }

   private void emitRobberyEvent(String eventType, Player robber, Player victim, RobberySession session,
                                 String reason, StaffBoardSeverity severity) {
      StaffBoardMetadata metadata = StaffBoardMetadata.create()
         .put("robber_uuid", robber.getUniqueId())
         .put("robber_name", robber.getName())
         .put("victim_uuid", victim.getUniqueId())
         .put("victim_name", victim.getName())
         .put("robbery_type", "player")
         .put("reason", reason)
         .put("started_at", session.getStartTime())
         .put("source_system", "OpenWeapons")
         .putLocation(session.getStartLocation());

      this.module.getStaffLogBridge().emit(StaffBoardLogEvent.builder(eventType, "OpenWeapons")
         .category(StaffBoardCategory.CRIME)
         .severity(severity)
         .sensitivity(StaffBoardSensitivity.SENSITIVE)
         .actor(robber)
         .target(victim)
         .location(session.getStartLocation())
         .message(robber.getName() + " robbery event involving " + victim.getName() + ".")
         .metadataJson(metadata.toJson())
         .build());
   }
}
