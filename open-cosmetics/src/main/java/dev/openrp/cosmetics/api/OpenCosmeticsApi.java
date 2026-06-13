package dev.openrp.cosmetics.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public interface OpenCosmeticsApi {
    String TYPE_LED = "led";
    String TYPE_COLOR = "color";
    String TYPE_SKIN = "skin";
    String NONE = "none";
    String SOUND_FIRE = "fire";
    String SOUND_HIT = "hit";
    String SOUND_HEADSHOT = "headshot";
    String SOUND_RELOAD = "reload";
    String SOUND_AUTOMATIC = "automatic";

    void reload();

    boolean supportsWeapon(String weaponId);

    boolean supportsWeaponCosmeticType(String weaponId, String type);

    List<String> getSkinIds(String weaponId);

    List<String> getLedIds();

    List<String> getColorIds();

    String getSkinDisplayName(String weaponId, String skinId);

    String getLedDisplayName(String ledId);

    String getColorDisplayName(String colorId);

    String getColorHex(String colorId);

    ItemStack createToken(String type, String id, int amount);

    boolean applySelection(ItemStack weaponItem, String skinId, String ledId, String color);

    void openWorkbench(Player player);

    void openEditor(Player player);

    String getWeaponSkinSound(ItemStack weaponItem, String soundKey);

    Component decorateWeaponDisplayName(ItemStack weaponItem, Component baseName);

    List<String> visualVariantCandidates(ItemStack weaponItem, boolean optic, boolean hasMagazine, boolean grip);

    Integer getWeaponColorRgb(ItemStack weaponItem);

    void applyVisualCustomModelData(ItemMeta meta, int customModelData, Integer rgb);

    void applyVisualDataComponents(ItemStack item, int customModelData, Integer rgb);

    void setAutomaticSkinFireSuppressed(UUID playerId, boolean suppressed);

    boolean isAutomaticSkinFireSuppressed(UUID playerId);
}
