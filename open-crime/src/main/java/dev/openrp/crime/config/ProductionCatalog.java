package dev.openrp.crime.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Recipes and location types, loaded from {@code production.yml}. No good name, substance or method
 * appears here in code - only "a good produced at a location type from these inputs over this time".
 */
public final class ProductionCatalog {

    private final Map<String, Recipe> recipes = new LinkedHashMap<>();
    private final Map<String, LocationType> locationTypes = new LinkedHashMap<>();

    public void load(ConfigurationSection root) {
        recipes.clear();
        locationTypes.clear();
        if (root == null) {
            return;
        }
        ConfigurationSection types = root.getConfigurationSection("location_types");
        if (types != null) {
            for (String id : types.getKeys(false)) {
                ConfigurationSection type = types.getConfigurationSection(id);
                if (type == null) {
                    continue;
                }
                locationTypes.put(id, new LocationType(
                        id,
                        type.getString("display_name", id),
                        type.getString("region_tag", ""),
                        type.getInt("max_concurrent", 1),
                        type.getBoolean("discoverable", true),
                        type.getInt("discovery_radius", 16),
                        type.getString("requires_item", "")));
            }
        }
        ConfigurationSection recipeRoot = root.getConfigurationSection("recipes");
        if (recipeRoot != null) {
            for (String id : recipeRoot.getKeys(false)) {
                ConfigurationSection recipe = recipeRoot.getConfigurationSection(id);
                if (recipe == null) {
                    continue;
                }
                List<RecipeStage> stages = new ArrayList<>();
                List<Map<?, ?>> stageMaps = recipe.getMapList("stages");
                for (Map<?, ?> stage : stageMaps) {
                    stages.add(readStage(stage));
                }
                recipes.put(id, new Recipe(id, recipe.getString("good_id", id), stages));
            }
        }
    }

    private RecipeStage readStage(Map<?, ?> stage) {
        String id = stringOr(stage.get("id"), "");
        String locationType = stringOr(stage.get("location_type"), "");
        int duration = intValue(stage.get("duration_minutes"), 0);
        int workers = intValue(stage.get("workers_required"), 0);
        String outputGood = stringOr(stage.get("output_good"), "");
        int outputGoodAmount = intValue(stage.get("output_good_amount"), 0);
        return new RecipeStage(id, locationType, duration,
                readIngredients(stage.get("inputs")), readIngredients(stage.get("outputs")),
                outputGood, outputGoodAmount, workers);
    }

    private List<RecipeIngredient> readIngredients(Object raw) {
        List<RecipeIngredient> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    result.add(new RecipeIngredient(
                            stringOr(map.get("item"), "AIR"),
                            intValue(map.get("amount"), 1)));
                }
            }
        }
        return result;
    }

    private static String stringOr(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    public Optional<Recipe> recipe(String id) {
        return Optional.ofNullable(id == null ? null : recipes.get(id));
    }

    public Collection<Recipe> recipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public List<String> recipeIds() {
        return List.copyOf(recipes.keySet());
    }

    public Optional<LocationType> locationType(String id) {
        return Optional.ofNullable(id == null ? null : locationTypes.get(id));
    }

    public Collection<LocationType> locationTypes() {
        return Collections.unmodifiableCollection(locationTypes.values());
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return fallback;
        }
    }
}
