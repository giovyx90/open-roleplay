package dev.openrp.cosmetics.api;

import org.bukkit.inventory.ItemStack;

public interface OpenCosmeticsWeaponBridge {
    String getWeaponId(ItemStack item);

    boolean isWeapon(ItemStack item);

    void refreshWeaponVisual(ItemStack item);
}
