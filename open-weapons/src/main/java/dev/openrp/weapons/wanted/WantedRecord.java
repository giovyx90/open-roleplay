package dev.openrp.weapons.wanted;

import java.time.Instant;
import java.util.UUID;

public class WantedRecord {
   private final UUID playerUuid;
   private final String playerName;
   private final String reason;
   private final boolean arrestRequired;
   private final UUID officerUuid;
   private final String officerName;
   private final Instant createdAt;

   public WantedRecord(UUID playerUuid, String playerName, String reason, boolean arrestRequired, UUID officerUuid, String officerName, Instant createdAt) {
      this.playerUuid = playerUuid;
      this.playerName = playerName;
      this.reason = reason;
      this.arrestRequired = arrestRequired;
      this.officerUuid = officerUuid;
      this.officerName = officerName;
      this.createdAt = createdAt;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public String getReason() {
      return this.reason;
   }

   public boolean isArrestRequired() {
      return this.arrestRequired;
   }

   public UUID getOfficerUuid() {
      return this.officerUuid;
   }

   public String getOfficerName() {
      return this.officerName;
   }

   public Instant getCreatedAt() {
      return this.createdAt;
   }
}
