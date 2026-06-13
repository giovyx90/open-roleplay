package dev.openrp.weapons.api;

import dev.openrp.weapons.model.WeaponDefinition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record WeaponImpactContext(
        Player shooter,
        WeaponDefinition weapon,
        ItemStack weaponItem,
        LivingEntity target,
        Location hitLocation,
        double damage,
        double distance,
        boolean headshot,
        boolean lethal) {
}
