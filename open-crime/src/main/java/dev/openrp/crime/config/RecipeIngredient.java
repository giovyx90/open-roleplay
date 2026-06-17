package dev.openrp.crime.config;

/**
 * A required input or plain (non-illegal) output of a production stage: a Bukkit material and an
 * amount. Illegal outputs are declared separately on the stage via {@code output_good}.
 *
 * @param material Bukkit material name
 * @param amount   stack count (>= 1)
 */
public record RecipeIngredient(String material, int amount) {

    public RecipeIngredient {
        material = material == null ? "AIR" : material;
        amount = Math.max(1, amount);
    }
}
