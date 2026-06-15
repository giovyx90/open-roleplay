package dev.openrp.weapons.cosmetics;

import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.model.WeaponVisualState;
import org.bukkit.Material;
import org.junit.Assert;
import org.junit.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WeaponVisualVariantResolverTest {

    @Test
    public void weaponDefinitionPrefersSkinVariantBeforeColorFallback() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 201);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.of(
                "empty", 201,
                "magazine", 203,
                "color", 9201,
                "magazine-color", 9203,
                "skin-sugarline-bakery", 9241,
                "magazine-skin-sugarline-bakery", 9243));

        WeaponDefinition ak47 = firearm("ak_47", WeaponCategory.ASSAULT_RIFLE, 201, visualStates, variants, 2);

        Assert.assertEquals(9243, ak47.resolveVisualModelData(WeaponVisualState.IDLE,
                List.of("magazine-skin-sugarline-bakery", "skin-sugarline-bakery",
                        "magazine-color", "color", "magazine", "empty"),
                true));
    }

    @Test
    public void weaponDefinitionUsesAttachmentSpecificVariantBeforeBase() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 5301);

        Map<WeaponVisualState, Map<String, Integer>> variants = new EnumMap<>(WeaponVisualState.class);
        variants.put(WeaponVisualState.IDLE, Map.ofEntries(
                Map.entry("empty", 5301),
                Map.entry("grip", 5304),
                Map.entry("optic", 5309),
                Map.entry("optic-grip", 5312),
                Map.entry("color", 9321),
                Map.entry("optic-grip-color", 9332),
                Map.entry("skin-sugarline-bakery", 9361),
                Map.entry("optic-grip-skin-sugarline-bakery", 9372)));

        WeaponDefinition ppk = firearm("ppk", WeaponCategory.PISTOL, 5301, visualStates, variants, 0);

        Assert.assertEquals(9372, ppk.resolveVisualModelData(WeaponVisualState.IDLE,
                List.of("optic-grip-skin-sugarline-bakery", "skin-sugarline-bakery",
                        "optic-grip-color", "color", "optic-grip", "empty"),
                false));
    }

    @Test
    public void weaponDefinitionFallsBackToStateBaseModel() {
        Map<WeaponVisualState, Integer> visualStates = new EnumMap<>(WeaponVisualState.class);
        visualStates.put(WeaponVisualState.IDLE, 81);
        visualStates.put(WeaponVisualState.AIMING, 1081);

        WeaponDefinition mp5 = firearm("mp5", WeaponCategory.SMG, 81, visualStates, Map.of(), 2);

        Assert.assertEquals(1083, mp5.resolveVisualModelData(WeaponVisualState.AIMING,
                List.of("missing-skin", "missing-color", "empty"),
                true));
    }

    private WeaponDefinition firearm(String id, WeaponCategory category, int customModelData,
                                     Map<WeaponVisualState, Integer> visualStates,
                                     Map<WeaponVisualState, Map<String, Integer>> variants,
                                     int magazineVisualOffset) {
        return new WeaponDefinition(id, id, category, Material.CROSSBOW, customModelData,
                visualStates, variants, magazineVisualOffset,
                5.0, 2.0, 4, 50, 30, 70,
                "9mm", "shoot", "reload", true,
                List.of(), null, 0.01, 1,
                4.0, 0.45, 1.75, 0.75, 4.0, 40, 80, 0.6);
    }
}
