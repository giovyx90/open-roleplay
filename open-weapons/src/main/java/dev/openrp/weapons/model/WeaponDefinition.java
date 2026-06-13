package dev.openrp.weapons.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WeaponDefinition {
    private final String id;
    private final String displayName;
    private final WeaponCategory category;
    private final Material material;
    private final int customModelData;
    private final Map<WeaponVisualState, Integer> visualStates;
    private final Map<WeaponVisualState, Map<String, Integer>> visualVariants;
    private final int magazineVisualOffset;
    private final int magazineModelData;

    // Gun stats
    private final double damage;
    private final double headshotMultiplier;
    private final int fireRateTicks;
    private final int reloadTimeTicks;
    private final int magazineSize;
    private final double maxDistance;
    private final String ammoType;
    private final String soundShoot;
    private final String soundReload;
    private final boolean automatic;
    private final List<FireMode> fireModes;
    private final Integer scopeZoomLevel; // Can be null
    private final double recoil; // Pitch kick per shot (radians-like fraction)
    private final int pelletCount; // Number of pellets per shot (buckshot shotguns)
    private final double hipfireSpreadDeg;
    private final double adsSpreadDeg;
    private final double movingSpreadMultiplier;
    private final double sneakSpreadMultiplier;
    private final double jumpSpreadMultiplier;
    private final double falloffStartDistance;
    private final double falloffEndDistance;
    private final double falloffMinMultiplier;

    // Melee stats
    private final double attackSpeed;
    private final double knockback;
    private final String soundHit;

    // Constructor for Guns
    public WeaponDefinition(String id, String displayName, WeaponCategory category, Material material, int customModelData,
                            Map<WeaponVisualState, Integer> visualStates,
                            Map<WeaponVisualState, Map<String, Integer>> visualVariants,
                            int magazineVisualOffset,
                            double damage, double headshotMultiplier, int fireRateTicks, int reloadTimeTicks, int magazineSize,
                            double maxDistance, String ammoType, String soundShoot, String soundReload, boolean automatic,
                            List<FireMode> fireModes, Integer scopeZoomLevel, double recoil, int pelletCount,
                            double hipfireSpreadDeg, double adsSpreadDeg, double movingSpreadMultiplier,
                            double sneakSpreadMultiplier, double jumpSpreadMultiplier, double falloffStartDistance,
                            double falloffEndDistance, double falloffMinMultiplier) {
        this(id, displayName, category, material, customModelData, visualStates, visualVariants,
                magazineVisualOffset, 0, damage, headshotMultiplier, fireRateTicks, reloadTimeTicks, magazineSize,
                maxDistance, ammoType, soundShoot, soundReload, automatic, fireModes, scopeZoomLevel, recoil,
                pelletCount, hipfireSpreadDeg, adsSpreadDeg, movingSpreadMultiplier, sneakSpreadMultiplier,
                jumpSpreadMultiplier, falloffStartDistance, falloffEndDistance, falloffMinMultiplier);
    }

    public WeaponDefinition(String id, String displayName, WeaponCategory category, Material material, int customModelData,
                            Map<WeaponVisualState, Integer> visualStates,
                            Map<WeaponVisualState, Map<String, Integer>> visualVariants,
                            int magazineVisualOffset,
                            int magazineModelData,
                            double damage, double headshotMultiplier, int fireRateTicks, int reloadTimeTicks, int magazineSize,
                            double maxDistance, String ammoType, String soundShoot, String soundReload, boolean automatic,
                            List<FireMode> fireModes, Integer scopeZoomLevel, double recoil, int pelletCount,
                            double hipfireSpreadDeg, double adsSpreadDeg, double movingSpreadMultiplier,
                            double sneakSpreadMultiplier, double jumpSpreadMultiplier, double falloffStartDistance,
                            double falloffEndDistance, double falloffMinMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.material = material;
        this.customModelData = customModelData;
        this.visualStates = visualStates != null && !visualStates.isEmpty()
                ? Collections.unmodifiableMap(new EnumMap<>(visualStates))
                : Collections.emptyMap();
        this.visualVariants = copyVisualVariants(visualVariants);
        this.magazineVisualOffset = magazineVisualOffset;
        this.magazineModelData = magazineModelData;
        
        this.damage = damage;
        this.headshotMultiplier = headshotMultiplier;
        this.fireRateTicks = fireRateTicks;
        this.reloadTimeTicks = reloadTimeTicks;
        this.magazineSize = magazineSize;
        this.maxDistance = maxDistance;
        this.ammoType = ammoType;
        this.soundShoot = soundShoot;
        this.soundReload = soundReload;
        this.automatic = automatic;
        this.fireModes = fireModes == null || fireModes.isEmpty()
                ? List.of(automatic ? FireMode.AUTO : FireMode.SEMI)
                : List.copyOf(fireModes);
        this.scopeZoomLevel = scopeZoomLevel;
        this.recoil = recoil;
        this.pelletCount = pelletCount;
        this.hipfireSpreadDeg = hipfireSpreadDeg;
        this.adsSpreadDeg = adsSpreadDeg;
        this.movingSpreadMultiplier = movingSpreadMultiplier;
        this.sneakSpreadMultiplier = sneakSpreadMultiplier;
        this.jumpSpreadMultiplier = jumpSpreadMultiplier;
        this.falloffStartDistance = falloffStartDistance;
        this.falloffEndDistance = falloffEndDistance;
        this.falloffMinMultiplier = falloffMinMultiplier;
        
        this.attackSpeed = 0;
        this.knockback = 0;
        this.soundHit = null;
    }

    // Constructor for Melee
    public WeaponDefinition(String id, String displayName, WeaponCategory category, Material material, int customModelData,
                            Map<WeaponVisualState, Integer> visualStates,
                            Map<WeaponVisualState, Map<String, Integer>> visualVariants,
                            int magazineVisualOffset,
                            double damage, double attackSpeed, double knockback, String soundHit) {
        this(id, displayName, category, material, customModelData, visualStates, visualVariants,
                magazineVisualOffset, 0, damage, attackSpeed, knockback, soundHit);
    }

    public WeaponDefinition(String id, String displayName, WeaponCategory category, Material material, int customModelData,
                            Map<WeaponVisualState, Integer> visualStates,
                            Map<WeaponVisualState, Map<String, Integer>> visualVariants,
                            int magazineVisualOffset,
                            int magazineModelData,
                            double damage, double attackSpeed, double knockback, String soundHit) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.material = material;
        this.customModelData = customModelData;
        this.visualStates = visualStates != null && !visualStates.isEmpty()
                ? Collections.unmodifiableMap(new EnumMap<>(visualStates))
                : Collections.emptyMap();
        this.visualVariants = copyVisualVariants(visualVariants);
        this.magazineVisualOffset = magazineVisualOffset;
        this.magazineModelData = magazineModelData;
        
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.knockback = knockback;
        this.soundHit = soundHit;
        
        this.headshotMultiplier = 1.0;
        this.fireRateTicks = 0;
        this.reloadTimeTicks = 0;
        this.magazineSize = 0;
        this.maxDistance = 0;
        this.ammoType = null;
        this.soundShoot = null;
        this.soundReload = null;
        this.automatic = false;
        this.fireModes = List.of();
        this.scopeZoomLevel = null;
        this.recoil = 0;
        this.pelletCount = 1;
        this.hipfireSpreadDeg = 0;
        this.adsSpreadDeg = 0;
        this.movingSpreadMultiplier = 1.0;
        this.sneakSpreadMultiplier = 1.0;
        this.jumpSpreadMultiplier = 1.0;
        this.falloffStartDistance = 0;
        this.falloffEndDistance = 0;
        this.falloffMinMultiplier = 1.0;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public WeaponCategory getCategory() { return category; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public double getDamage() { return damage; }
    public double getHeadshotMultiplier() { return headshotMultiplier; }
    public int getFireRateTicks() { return fireRateTicks; }
    public int getReloadTimeTicks() { return reloadTimeTicks; }
    public int getMagazineSize() { return magazineSize; }
    public double getMaxDistance() { return maxDistance; }
    public String getAmmoType() { return ammoType; }
    public String getSoundShoot() { return soundShoot; }
    public String getSoundReload() { return soundReload; }
    public boolean isAutomatic() { return automatic; }
    public List<FireMode> getFireModes() { return fireModes; }
    public boolean hasMultipleFireModes() { return fireModes.size() > 1; }
    public Integer getScopeZoomLevel() { return scopeZoomLevel; }
    public double getRecoil() { return recoil; }
    public int getPelletCount() { return pelletCount; }
    public double getHipfireSpreadDeg() { return hipfireSpreadDeg; }
    public double getAdsSpreadDeg() { return adsSpreadDeg; }
    public double getMovingSpreadMultiplier() { return movingSpreadMultiplier; }
    public double getSneakSpreadMultiplier() { return sneakSpreadMultiplier; }
    public double getJumpSpreadMultiplier() { return jumpSpreadMultiplier; }
    public double getFalloffStartDistance() { return falloffStartDistance; }
    public double getFalloffEndDistance() { return falloffEndDistance; }
    public double getFalloffMinMultiplier() { return falloffMinMultiplier; }
    public double getAttackSpeed() { return attackSpeed; }
    public double getKnockback() { return knockback; }
    public String getSoundHit() { return soundHit; }

    /**
     * Returns the CustomModelData for the given visual state, or -1 if
     * no specific model is defined for that state.
     */
    public int getVisualModelData(WeaponVisualState state) {
        Integer cmd = visualStates.get(state);
        return cmd != null ? cmd : -1;
    }

    /**
     * Returns the CustomModelData for a specific visual variant, or -1 if
     * that exact state/variant pair is not configured.
     */
    public int getVisualVariantModelData(WeaponVisualState state, String variant) {
        Map<String, Integer> variants = visualVariants.get(state);
        if (variants == null || variants.isEmpty()) {
            return -1;
        }
        Integer cmd = variants.get(normalizeVisualVariantKey(variant));
        return cmd != null ? cmd : -1;
    }

    public int resolveVisualModelData(WeaponVisualState state, List<String> variantCandidates, boolean hasMagazine) {
        List<String> candidates = variantCandidates == null || variantCandidates.isEmpty()
                ? List.of("empty")
                : variantCandidates;
        for (String variant : candidates) {
            int cmd = getVisualVariantModelData(state, variant);
            if (cmd > 0) {
                return cmd;
            }
        }
        if (state != WeaponVisualState.IDLE) {
            for (String variant : candidates) {
                int cmd = getVisualVariantModelData(WeaponVisualState.IDLE, variant);
                if (cmd > 0) {
                    return cmd;
                }
            }
        }

        int cmd = getVisualModelData(state);
        if (cmd < 0) {
            cmd = getVisualModelData(WeaponVisualState.IDLE);
        }
        if (cmd < 0) {
            cmd = getCustomModelData();
        }
        if (cmd <= 0) {
            return cmd;
        }
        return hasMagazine && getMagazineVisualOffset() > 0 ? cmd + getMagazineVisualOffset() : cmd;
    }

    /** Whether this weapon defines a model for the given visual state. */
    public boolean hasVisualState(WeaponVisualState state) {
        return visualStates.containsKey(state) || visualVariants.containsKey(state);
    }

    /** Whether this weapon has any visual states defined at all. */
    public boolean hasAnyVisualState() {
        return !visualStates.isEmpty() || !visualVariants.isEmpty();
    }

    /**
     * Offset added to the current visual state CMD when the weapon has
     * a magazine inserted. 0 means no magazine variant exists.
     */
    public int getMagazineVisualOffset() {
        return magazineVisualOffset;
    }

    public int getMagazineModelData() {
        return magazineModelData;
    }

    public static String normalizeVisualVariantKey(String variant) {
        if (variant == null || variant.isBlank()) {
            return "empty";
        }
        return variant.trim().toLowerCase().replace('_', '-');
    }

    private static Map<WeaponVisualState, Map<String, Integer>> copyVisualVariants(
            Map<WeaponVisualState, Map<String, Integer>> visualVariants) {
        if (visualVariants == null || visualVariants.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<WeaponVisualState, Map<String, Integer>> copy = new EnumMap<>(WeaponVisualState.class);
        for (Map.Entry<WeaponVisualState, Map<String, Integer>> entry : visualVariants.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            copy.put(entry.getKey(), Collections.unmodifiableMap(new java.util.HashMap<>(entry.getValue())));
        }
        return copy.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copy);
    }
}
