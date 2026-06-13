package dev.openrp.weapons.api;

import dev.openrp.weapons.model.WeaponDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record WeaponUseContext(
        Player shooter,
        WeaponDefinition weapon,
        ItemStack weaponItem) {
}
