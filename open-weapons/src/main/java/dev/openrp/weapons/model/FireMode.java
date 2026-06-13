package dev.openrp.weapons.model;

import java.util.Locale;

public enum FireMode {
   SEMI("Semi"),
   AUTO("Auto"),
   BURST("Raffica");

   private final String displayName;

   FireMode(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public static FireMode fromConfig(String value) {
      if (value == null) {
         return SEMI;
      }

      return switch (value.trim().toLowerCase(Locale.ROOT)) {
         case "auto", "automatic", "full_auto", "full-auto" -> AUTO;
         case "burst", "raffica" -> BURST;
         default -> SEMI;
      };
   }
}
