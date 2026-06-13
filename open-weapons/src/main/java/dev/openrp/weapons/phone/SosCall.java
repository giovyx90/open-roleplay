package dev.openrp.weapons.phone;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.Location;

public class SosCall {
   private final String id;
   private final SosCall.Service service;
   private final UUID callerUuid;
   private final String callerName;
   private final Location location;
   private final String reason;
   private final Instant createdAt;

   public SosCall(String id, SosCall.Service service, UUID callerUuid, String callerName, Location location, String reason, Instant createdAt) {
      this.id = id;
      this.service = service;
      this.callerUuid = callerUuid;
      this.callerName = callerName;
      this.location = location;
      this.reason = reason;
      this.createdAt = createdAt;
   }

   public String getId() {
      return this.id;
   }

   public SosCall.Service getService() {
      return this.service;
   }

   public UUID getCallerUuid() {
      return this.callerUuid;
   }

   public String getCallerName() {
      return this.callerName;
   }

   public Location getLocation() {
      return this.location;
   }

   public String getReason() {
      return this.reason;
   }

   public Instant getCreatedAt() {
      return this.createdAt;
   }

   public enum Service {
      POLICE("Police", "LAW_ENFORCEMENT"),
      HOSPITAL("Hospital", "HOSPITAL"),
      FIRE_DEPARTMENT("Fire Department", "FIRE_DEPARTMENT");

      private final String displayName;
      private final String companyType;

      Service(String displayName, String companyType) {
         this.displayName = displayName;
         this.companyType = companyType;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getCompanyType() {
         return this.companyType;
      }
   }
}
