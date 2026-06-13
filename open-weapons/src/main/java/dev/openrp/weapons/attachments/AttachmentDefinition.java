package dev.openrp.weapons.attachments;

import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponCategory;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Set;

public class AttachmentDefinition {
    private final String id;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final AttachmentSlot slot;
    private final Set<WeaponCategory> compatibleCategories;
    private final Set<String> compatibleWeaponIds;
    private final double recoilMultiplier;
    private final double spreadMultiplier;
    private final double adsSpreadMultiplier;
    private final double hipfireSpreadMultiplier;
    private final double soundMultiplier;
    private final double maxDistanceMultiplier;
    private final double mobilityMultiplier;
    private final double reloadTimeMultiplier;
    private final int zoomBonus;
    private final int installTimeTicks;
    private final boolean illegal;

    public AttachmentDefinition(String id, String displayName, Material material, int customModelData,
                                AttachmentSlot slot, Set<WeaponCategory> compatibleCategories,
                                Set<String> compatibleWeaponIds,
                                double recoilMultiplier, double spreadMultiplier, double soundMultiplier,
                                double maxDistanceMultiplier, double adsSpreadMultiplier,
                                double hipfireSpreadMultiplier, double mobilityMultiplier,
                                double reloadTimeMultiplier, int zoomBonus, int installTimeTicks, boolean illegal) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.slot = slot;
        this.compatibleCategories = Set.copyOf(compatibleCategories);
        this.compatibleWeaponIds = Set.copyOf(compatibleWeaponIds);
        this.recoilMultiplier = recoilMultiplier;
        this.spreadMultiplier = spreadMultiplier;
        this.adsSpreadMultiplier = adsSpreadMultiplier;
        this.hipfireSpreadMultiplier = hipfireSpreadMultiplier;
        this.soundMultiplier = soundMultiplier;
        this.maxDistanceMultiplier = maxDistanceMultiplier;
        this.mobilityMultiplier = mobilityMultiplier;
        this.reloadTimeMultiplier = reloadTimeMultiplier;
        this.zoomBonus = zoomBonus;
        this.installTimeTicks = installTimeTicks;
        this.illegal = illegal;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public AttachmentSlot getSlot() {
        return slot;
    }

    public Set<WeaponCategory> getCompatibleCategories() {
        return compatibleCategories;
    }

    public Set<String> getCompatibleWeaponIds() {
        return compatibleWeaponIds;
    }

    public boolean isCompatibleWith(WeaponDefinition weapon) {
        if (weapon == null || !compatibleCategories.contains(weapon.getCategory())) {
            return false;
        }
        return compatibleWeaponIds.isEmpty()
                || compatibleWeaponIds.contains(weapon.getId().toLowerCase(Locale.ROOT));
    }

    public double getRecoilMultiplier() {
        return recoilMultiplier;
    }

    public double getSpreadMultiplier() {
        return spreadMultiplier;
    }

    public double getAdsSpreadMultiplier() {
        return adsSpreadMultiplier;
    }

    public double getHipfireSpreadMultiplier() {
        return hipfireSpreadMultiplier;
    }

    public double getSoundMultiplier() {
        return soundMultiplier;
    }

    public double getMaxDistanceMultiplier() {
        return maxDistanceMultiplier;
    }

    public double getMobilityMultiplier() {
        return mobilityMultiplier;
    }

    public double getReloadTimeMultiplier() {
        return reloadTimeMultiplier;
    }

    public int getZoomBonus() {
        return zoomBonus;
    }

    public int getInstallTimeTicks() {
        return installTimeTicks;
    }

    public boolean isIllegal() {
        return illegal;
    }
}
