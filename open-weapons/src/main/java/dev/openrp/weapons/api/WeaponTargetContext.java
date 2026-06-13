package dev.openrp.weapons.api;

import dev.openrp.weapons.model.WeaponDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record WeaponTargetContext(
        Player shooter,
        WeaponDefinition weapon,
        ItemStack weaponItem,
        LivingEntity target) {
}
