package dev.openrp.weapons.grenades;

import org.bukkit.Material;

public class GrenadeDefinition {
    private final String id;
    private final String displayName;
    private final GrenadeType type;
    private final Material material;
    private final int customModelData;
    private final int fuseTimeTicks;
    private final double radius;
    private final double damage;
    private final int effectDurationTicks;

    public GrenadeDefinition(String id, String displayName, GrenadeType type, Material material, int customModelData,
                             int fuseTimeTicks, double radius, double damage, int effectDurationTicks) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.material = material;
        this.customModelData = customModelData;
        this.fuseTimeTicks = fuseTimeTicks;
        this.radius = radius;
        this.damage = damage;
        this.effectDurationTicks = effectDurationTicks;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public GrenadeType getType() { return type; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public int getFuseTimeTicks() { return fuseTimeTicks; }
    public double getRadius() { return radius; }
    public double getDamage() { return damage; }
    public int getEffectDurationTicks() { return effectDurationTicks; }
}
