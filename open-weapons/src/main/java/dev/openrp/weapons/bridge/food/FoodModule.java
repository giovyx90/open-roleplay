package dev.openrp.weapons.bridge.food;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FoodModule {
    FoodConfig config();

    FreshnessService freshnessService();

    interface FoodConfig {
        Map<String, FoodRecipe> recipes();

        MachineSettings machineSettings();

        AlcoholSettings alcoholSettings();
    }

    interface MachineSettings {
        Map<String, FoodWorkstation> nexoWorkstations();

        Set<String> visualOnlyNexoItems();
    }

    interface AlcoholSettings {
        boolean isAlcoholProductionWorkstation(FoodWorkstation workstation);
    }

    interface FreshnessService {
        ItemStack createFoodItem(ItemStack output, Player player, boolean admin, NamedTextColor color);

        Optional<Object> read(ItemStack item);
    }
}
