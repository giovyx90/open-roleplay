package dev.openrp.weapons.robbery;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

public class RobberySession {
   private final UUID robberUuid;
   private final UUID victimUuid;
   private final long startTime;
   private final Location startLocation;
   private TextDisplay textDisplay;
   private BukkitTask task;
   private BukkitTask distanceTask;
   private boolean markedAbbattibile = false;

   public RobberySession(UUID robberUuid, UUID victimUuid, long startTime, Location startLocation) {
      this.robberUuid = robberUuid;
      this.victimUuid = victimUuid;
      this.startTime = startTime;
      this.startLocation = startLocation;
   }

   public UUID getRobberUuid() {
      return this.robberUuid;
   }

   public UUID getVictimUuid() {
      return this.victimUuid;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public Location getStartLocation() {
      return this.startLocation;
   }

   public TextDisplay getTextDisplay() {
      return this.textDisplay;
   }

   public void setTextDisplay(TextDisplay textDisplay) {
      this.textDisplay = textDisplay;
   }

   public BukkitTask getTask() {
      return this.task;
   }

   public void setTask(BukkitTask task) {
      this.task = task;
   }

   public BukkitTask getDistanceTask() {
      return this.distanceTask;
   }

   public void setDistanceTask(BukkitTask distanceTask) {
      this.distanceTask = distanceTask;
   }

   public boolean isMarkedAbbattibile() {
      return this.markedAbbattibile;
   }

   public void setMarkedAbbattibile(boolean markedAbbattibile) {
      this.markedAbbattibile = markedAbbattibile;
   }
}
