package dev.openrp.weapons.robbery;

import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
import it.meridian.core.permissions.NextPermissions;
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
   private final Map<UUID, TextDisplay> killableTags = new HashMap<>();
   private static final int MAX_DAILY_ROBBERIES = 2;
   private static final int ROBBERY_DURATION_TICKS = 6000;
   private static final double FLEE_DISTANCE = 10.0;
   private static final int KILLABLE_SECONDS = 300;

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
      return NextPermissions.hasAny(player,
            NextPermissions.Robbery.ADMIN,
            NextPermissions.Weapons.ADMIN,
            NextPermissions.Staff.ADMIN,
            NextPermissions.Test.DEBUG);
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
            } else if (session.isMarkedKillable()) {
               this.cancel();
            } else if (!victim.getWorld().equals(session.getStartLocation().getWorld())) {
               RobberyManager.this.applyFleeKillable(victim, robber, session);
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
                  RobberyManager.this.applyFleeKillable(victim, robber, session);
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

   private void applyFleeKillable(final Player victim, Player robber, RobberySession session) {
      session.setMarkedKillable(true);
      this.endRobbery(victim.getUniqueId());
      emitRobberyEvent("crime.robbery.failed", robber, victim, session, "La vittima e' fuggita dalla rapina.",
              StaffBoardSeverity.WARNING);
      robber.sendMessage(Component.text(victim.getName() + " e' fuggito dalla rapina! Ora e' UCCIDIBILE per 5 minuti.", NamedTextColor.GREEN));
      victim.sendMessage(
         Component.text("Sei fuggito dalla rapina! Ora sei UCCIDIBILE per 5 minuti.", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      double tagOffset = module.getUtilitySettings().statusTagYOffset();
      final TextDisplay td = StatusTextDisplays.spawn(victim,
            Component.text("☠ KILLABLE - 5:00 ☠", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD}),
            tagOffset);
      this.killableTags.put(victim.getUniqueId(), td);
      int KILLABLE_TICKS = 6000;
      (new BukkitRunnable() {
            int ticks = 0;

            public void run() {
               if (this.ticks < 6000 && victim.isOnline()) {
                  if (!victim.isDead() && td.isValid()) {
                     int secondsLeft = (6000 - this.ticks) / 20;
                     int mins = secondsLeft / 60;
                     int secs = secondsLeft % 60;
                     td.text(
                        Component.text(
                           "☠ KILLABLE - " + mins + ":" + String.format("%02d", secs) + " ☠",
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

                  RobberyManager.this.killableTags.remove(victim.getUniqueId());
                  if (victim.isOnline()) {
                     victim.sendMessage(Component.text("Non sei piu' uccidibile.", NamedTextColor.GREEN));
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

      this.module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder(eventType, "OpenWeapons")
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
