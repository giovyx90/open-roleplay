package dev.openrp.weapons.bridge.food;

import org.bukkit.inventory.ItemStack;

public interface FoodRecipe {
    String id();

    String displayName();

    ItemStack output();

    FoodWorkstation workstation();

    double alcoholDelta();

    int hydrationDelta();

    int hungerRestore();
}
