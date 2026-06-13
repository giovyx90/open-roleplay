package dev.openrp.weapons.model;

public class HelmetDefinition {
   private final String id;
   private final String displayName;
   private final int customModelData;
   private final double damageReduction;
   private final boolean negatesHeadshot;
   private final boolean preventsMeleeStun;
   private final int maxDurability;
   private final int colorRgb;

   public HelmetDefinition(
      String id, String displayName, int customModelData, double damageReduction, boolean negatesHeadshot, boolean preventsMeleeStun, int maxDurability
   ) {
      this(id, displayName, customModelData, damageReduction, negatesHeadshot, preventsMeleeStun, maxDurability, -1);
   }

   public HelmetDefinition(
      String id, String displayName, int customModelData, double damageReduction, boolean negatesHeadshot, boolean preventsMeleeStun,
      int maxDurability, int colorRgb
   ) {
      this.id = id;
      this.displayName = displayName;
      this.customModelData = customModelData;
      this.damageReduction = damageReduction;
      this.negatesHeadshot = negatesHeadshot;
      this.preventsMeleeStun = preventsMeleeStun;
      this.maxDurability = maxDurability;
      this.colorRgb = colorRgb;
   }

   public String getId() {
      return this.id;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public int getCustomModelData() {
      return this.customModelData;
   }

   public double getDamageReduction() {
      return this.damageReduction;
   }

   public boolean negatesHeadshot() {
      return this.negatesHeadshot;
   }

   public boolean preventsMeleeStun() {
      return this.preventsMeleeStun;
   }

   public int getMaxDurability() {
      return this.maxDurability;
   }

   public int getColorRgb() {
      return this.colorRgb;
   }
}
