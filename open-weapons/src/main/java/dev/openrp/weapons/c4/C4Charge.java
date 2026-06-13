package dev.openrp.weapons.c4;

import dev.openrp.weapons.grenades.GrenadeDefinition;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitTask;

public class C4Charge {
   private final String id;
   private final GrenadeDefinition definition;
   private final UUID ownerUuid;
   private final String ownerName;
   private final Location location;
   private final Instant createdAt;
   private int fuseSeconds;
   private Instant detonatesAt;
   private ItemDisplay display;
   private BukkitTask detonationTask;
   private boolean detonating;

   public C4Charge(String id, GrenadeDefinition definition, UUID ownerUuid, String ownerName, Location location, Instant createdAt, int fuseSeconds) {
      this.id = id;
      this.definition = definition;
      this.ownerUuid = ownerUuid;
      this.ownerName = ownerName;
      this.location = location;
      this.createdAt = createdAt;
      this.fuseSeconds = fuseSeconds;
      this.detonatesAt = createdAt.plusSeconds(fuseSeconds);
   }

   public String getId() {
      return this.id;
   }

   public GrenadeDefinition getDefinition() {
      return this.definition;
   }

   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public String getOwnerName() {
      return this.ownerName;
   }

   public Location getLocation() {
      return this.location;
   }

   public Instant getCreatedAt() {
      return this.createdAt;
   }

   public int getFuseSeconds() {
      return this.fuseSeconds;
   }

   public void setFuseSeconds(int fuseSeconds) {
      this.fuseSeconds = fuseSeconds;
      this.detonatesAt = Instant.now().plusSeconds(fuseSeconds);
   }

   public Instant getDetonatesAt() {
      return this.detonatesAt;
   }

   public long getRemainingSeconds() {
      return Math.max(0L, this.detonatesAt.getEpochSecond() - Instant.now().getEpochSecond());
   }

   public ItemDisplay getDisplay() {
      return this.display;
   }

   public void setDisplay(ItemDisplay display) {
      this.display = display;
   }

   public BukkitTask getDetonationTask() {
      return this.detonationTask;
   }

   public void setDetonationTask(BukkitTask detonationTask) {
      this.detonationTask = detonationTask;
   }

   public boolean isDetonating() {
      return this.detonating;
   }

   public void setDetonating(boolean detonating) {
      this.detonating = detonating;
   }
}
