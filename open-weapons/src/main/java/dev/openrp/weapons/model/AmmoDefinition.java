package dev.openrp.weapons.model;

import org.bukkit.Material;

public class AmmoDefinition {
    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final int maxStack;
    private final String penetrationClass;
    private final int armorDurabilityDamage;
    private final double fleshDamageMultiplier;
    private final int shieldDurabilityDamage;

    public AmmoDefinition(String id, String displayName, Material material, int customModelData, int maxStack,
                          String penetrationClass, int armorDurabilityDamage, double fleshDamageMultiplier,
                          int shieldDurabilityDamage) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.maxStack = maxStack;
        this.penetrationClass = penetrationClass;
        this.armorDurabilityDamage = armorDurabilityDamage;
        this.fleshDamageMultiplier = fleshDamageMultiplier;
        this.shieldDurabilityDamage = shieldDurabilityDamage;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public int getMaxStack() { return maxStack; }
    public String getPenetrationClass() { return penetrationClass; }
    public int getArmorDurabilityDamage() { return armorDurabilityDamage; }
    public double getFleshDamageMultiplier() { return fleshDamageMultiplier; }
    public int getShieldDurabilityDamage() { return shieldDurabilityDamage; }
}
