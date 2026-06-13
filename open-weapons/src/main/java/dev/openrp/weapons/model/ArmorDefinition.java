package dev.openrp.weapons.model;

/**
 * Represents a bulletproof vest definition.
 * - Light: No slowness, 25% damage reduction, 50 durability.
 * - Heavy: Slowness I, 45% damage reduction, 100 durability.
 * - Heavy Plated: Slowness I, 45% damage reduction, 125 durability (plate adds 25).
 *   When the plate breaks the vest reverts to a normal heavy vest.
 */
public class ArmorDefinition {
    private final String id;
    private final String displayName;
    private final int customModelData;
    private final int slownessLevel;         // -1 = no slowness, 0 = Slowness I, 1 = Slowness II
    private final double damageReduction;    // 0.25 = 25% reduction, 0.45 = 45% reduction
    private final String nijLevel;           // "IIIA" or "IV" for display
    private final int maxDurability;         // Total hits before breaking
    private final boolean hasPlate;          // Whether this vest has a ceramic plate
    private final int colorRgb;              // -1 = use legacy/default color

    public ArmorDefinition(String id, String displayName, int customModelData,
                           int slownessLevel, double damageReduction, String nijLevel,
                           int maxDurability, boolean hasPlate) {
        this(id, displayName, customModelData, slownessLevel, damageReduction, nijLevel, maxDurability, hasPlate, -1);
    }

    public ArmorDefinition(String id, String displayName, int customModelData,
                           int slownessLevel, double damageReduction, String nijLevel,
                           int maxDurability, boolean hasPlate, int colorRgb) {
        this.id = id;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.slownessLevel = slownessLevel;
        this.damageReduction = damageReduction;
        this.nijLevel = nijLevel;
        this.maxDurability = maxDurability;
        this.hasPlate = hasPlate;
        this.colorRgb = colorRgb;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCustomModelData() { return customModelData; }
    public int getSlownessLevel() { return slownessLevel; }
    public boolean hasSlowness() { return slownessLevel >= 0; }
    public double getDamageReduction() { return damageReduction; }
    public String getNijLevel() { return nijLevel; }
    public int getMaxDurability() { return maxDurability; }
    public boolean hasPlate() { return hasPlate; }
    public int getColorRgb() { return colorRgb; }
}
