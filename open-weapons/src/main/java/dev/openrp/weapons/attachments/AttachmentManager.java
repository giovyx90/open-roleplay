package dev.openrp.weapons.attachments;

import dev.openrp.weapons.cosmetics.WeaponCosmeticManager;
import dev.openrp.weapons.cosmetics.WeaponVisualVariantResolver;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponVisualState;
import dev.openrp.weapons.module.WeaponsModule;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttachmentManager {
    private static final double MIN_RECOIL_MULTIPLIER = 0.65D;
    private static final double MIN_SPREAD_MULTIPLIER = 0.65D;
    private static final double MIN_SOUND_MULTIPLIER = 0.25D;
    private static final double MIN_MAX_DISTANCE_MULTIPLIER = 0.80D;
    private static final double MIN_RELOAD_TIME_MULTIPLIER = 0.60D;
    private static final double MIN_MOBILITY_MULTIPLIER = 0.50D;

    private final WeaponsModule module;
    private final AttachmentRegistry registry;
    private final Map<AttachmentSlot, NamespacedKey> attachmentKeys = new EnumMap<>(AttachmentSlot.class);
    private final NamespacedKey weaponVisualStateKey;
    private final NamespacedKey weaponHasMagazineVisualKey;

    public AttachmentManager(WeaponsModule module, AttachmentRegistry registry) {
        this.module = module;
        this.registry = registry;
        this.weaponVisualStateKey = new NamespacedKey(module.getCore(), "weapon_visual_state");
        this.weaponHasMagazineVisualKey = new NamespacedKey(module.getCore(), "weapon_has_magazine_visual");
        for (AttachmentSlot slot : AttachmentSlot.values()) {
            attachmentKeys.put(slot, new NamespacedKey(module.getCore(), "weapon_attachment_" + slot.getId()));
        }
    }

    public String getAttachmentId(ItemStack weaponItem, AttachmentSlot slot) {
        if (weaponItem == null || weaponItem.getType().isAir() || slot == null || !weaponItem.hasItemMeta()) {
            return null;
        }
        return weaponItem.getItemMeta().getPersistentDataContainer().get(attachmentKeys.get(slot), PersistentDataType.STRING);
    }

    public AttachmentDefinition getAttachment(ItemStack weaponItem, AttachmentSlot slot) {
        return registry.getAttachment(getAttachmentId(weaponItem, slot));
    }

    public boolean hasAttachment(ItemStack weaponItem, AttachmentSlot slot, String attachmentId) {
        if (attachmentId == null) {
            return false;
        }
        String installedId = getAttachmentId(weaponItem, slot);
        return attachmentId.equalsIgnoreCase(installedId);
    }

    public Map<AttachmentSlot, AttachmentDefinition> getInstalledAttachments(ItemStack weaponItem) {
        Map<AttachmentSlot, AttachmentDefinition> installed = new EnumMap<>(AttachmentSlot.class);
        for (AttachmentSlot slot : AttachmentSlot.values()) {
            AttachmentDefinition attachment = getAttachment(weaponItem, slot);
            if (attachment != null) {
                installed.put(slot, attachment);
            }
        }
        return installed;
    }

    public boolean canInstall(ItemStack weaponItem, WeaponDefinition weapon, AttachmentDefinition attachment) {
        if (weaponItem == null || weaponItem.getType().isAir() || weapon == null || attachment == null) {
            return false;
        }
        WeaponDefinition itemWeapon = module.getWeaponRegistry().getWeapon(weaponItem);
        if (itemWeapon == null || !itemWeapon.getId().equals(weapon.getId())) {
            return false;
        }
        if (!attachment.isCompatibleWith(weapon)) {
            return false;
        }
        return getAttachmentId(weaponItem, attachment.getSlot()) == null;
    }

    public boolean installAttachment(ItemStack weaponItem, WeaponDefinition weapon, AttachmentDefinition attachment) {
        if (!canInstall(weaponItem, weapon, attachment)) {
            return false;
        }

        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return false;
        }
        meta.getPersistentDataContainer().set(attachmentKeys.get(attachment.getSlot()), PersistentDataType.STRING, attachment.getId());
        weaponItem.setItemMeta(meta);
        updateWeaponLore(weaponItem, weapon);
        refreshWeaponVisual(weaponItem, weapon);
        return true;
    }

    public AttachmentDefinition removeAttachment(ItemStack weaponItem, AttachmentSlot slot) {
        if (weaponItem == null || weaponItem.getType().isAir() || slot == null || !weaponItem.hasItemMeta()) {
            return null;
        }

        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(weaponItem);
        String installedId = getAttachmentId(weaponItem, slot);
        if (installedId == null) {
            return null;
        }
        AttachmentDefinition removed = registry.getAttachment(installedId);

        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.getPersistentDataContainer().remove(attachmentKeys.get(slot));
        weaponItem.setItemMeta(meta);
        if (weapon != null) {
            updateWeaponLore(weaponItem, weapon);
            refreshWeaponVisual(weaponItem, weapon);
        }
        return removed;
    }

    public double getRecoilMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getRecoilMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_RECOIL_MULTIPLIER, multiplier);
    }

    public double getSpreadMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getSpreadMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_SPREAD_MULTIPLIER, multiplier);
    }

    public double getAdsSpreadMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getAdsSpreadMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_SPREAD_MULTIPLIER, multiplier);
    }

    public double getHipfireSpreadMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getHipfireSpreadMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_SPREAD_MULTIPLIER, multiplier);
    }

    public double getSoundMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getSoundMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_SOUND_MULTIPLIER, multiplier);
    }

    public double getMaxDistanceMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getMaxDistanceMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_MAX_DISTANCE_MULTIPLIER, multiplier);
    }

    public double getMobilityMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getMobilityMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_MOBILITY_MULTIPLIER, multiplier);
    }

    public double getReloadTimeMultiplier(ItemStack weaponItem) {
        double multiplier = getInstalledAttachments(weaponItem).values().stream()
                .mapToDouble(AttachmentDefinition::getReloadTimeMultiplier)
                .reduce(1.0D, (left, right) -> left * right);
        return Math.max(MIN_RELOAD_TIME_MULTIPLIER, multiplier);
    }

    public int getZoomBonus(ItemStack weaponItem) {
        return getInstalledAttachments(weaponItem).values().stream()
                .mapToInt(AttachmentDefinition::getZoomBonus)
                .sum();
    }

    public void updateWeaponLore(ItemStack weaponItem, WeaponDefinition weapon) {
        updateWeaponLore(weaponItem, weapon, null);
    }

    public void updateWeaponLore(ItemStack weaponItem, WeaponDefinition weapon, String shotsText) {
        if (weaponItem == null || weapon == null) {
            return;
        }
        String modsDisplay = getInstalledAttachments(weaponItem).values().stream()
                .map(AttachmentDefinition::getDisplayName)
                .collect(Collectors.joining(" / "));
        if (modsDisplay.isBlank()) {
            modsDisplay = "None";
        }
        if (shotsText == null || shotsText.isBlank()) {
            module.getWeaponRegistry().updateWeaponLore(weaponItem, weapon, modsDisplay);
        } else {
            module.getWeaponRegistry().updateWeaponLore(weaponItem, weapon, modsDisplay, shotsText);
        }
    }

    public void refreshWeaponVisual(ItemStack weaponItem, WeaponDefinition weapon) {
        if (weaponItem == null || weapon == null || !weapon.hasAnyVisualState() || !weaponItem.hasItemMeta()) {
            return;
        }
        ItemMeta meta = weaponItem.getItemMeta();
        if (meta == null) {
            return;
        }
        WeaponVisualState visual = parseVisualState(meta);
        boolean hasMagazine = hasMagazineVisual(meta, weapon);
        int customModelData = weapon.resolveVisualModelData(visual, getVisualVariantCandidates(weaponItem, hasMagazine), hasMagazine);
        if (customModelData <= 0) {
            return;
        }
        WeaponCosmeticManager cosmetics = module.getWeaponCosmeticManager();
        if (cosmetics != null) {
            Integer rgb = cosmetics.getWeaponColorRgb(weaponItem);
            cosmetics.applyVisualCustomModelData(meta, customModelData, rgb);
            weaponItem.setItemMeta(meta);
            cosmetics.applyVisualDataComponents(weaponItem, customModelData, rgb);
            return;
        } else {
            meta.setCustomModelData(customModelData);
        }
        weaponItem.setItemMeta(meta);
    }

    private WeaponVisualState parseVisualState(ItemMeta meta) {
        String stored = meta.getPersistentDataContainer().get(weaponVisualStateKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return WeaponVisualState.IDLE;
        }
        try {
            return WeaponVisualState.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return WeaponVisualState.IDLE;
        }
    }

    private boolean hasMagazineVisual(ItemMeta meta, WeaponDefinition weapon) {
        Byte stored = meta.getPersistentDataContainer().get(weaponHasMagazineVisualKey, PersistentDataType.BYTE);
        if (stored != null) {
            return stored == (byte) 1;
        }
        return weapon.getMagazineVisualOffset() > 0;
    }

    private List<String> getVisualVariantCandidates(ItemStack item, boolean hasMagazine) {
        boolean optic = getAttachment(item, AttachmentSlot.OPTIC) != null;
        boolean grip = getAttachment(item, AttachmentSlot.UNDERBARREL) != null;
        WeaponCosmeticManager cosmetics = module.getWeaponCosmeticManager();
        String led = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponLed(item);
        String color = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponColor(item);
        String skin = cosmetics == null ? WeaponCosmeticManager.NONE : cosmetics.getWeaponSkin(item);
        return WeaponVisualVariantResolver.candidates(optic, hasMagazine, grip, led, color, skin);
    }
}
