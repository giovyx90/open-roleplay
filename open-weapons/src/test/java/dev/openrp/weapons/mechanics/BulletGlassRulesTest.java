package dev.openrp.weapons.mechanics;

import java.util.List;
import org.bukkit.Material;
import org.junit.Assert;
import org.junit.Test;

public class BulletGlassRulesTest {
    @Test
    public void defaultPredicateIncludesGlassVariantsOnly() {
        Assert.assertTrue(BulletGlassRules.isBreakableGlass(Material.GLASS, List.of()));
        Assert.assertTrue(BulletGlassRules.isBreakableGlass(Material.BLUE_STAINED_GLASS_PANE, List.of()));
        Assert.assertTrue(BulletGlassRules.isBreakableGlass(Material.TINTED_GLASS, List.of()));
        Assert.assertFalse(BulletGlassRules.isBreakableGlass(Material.IRON_BARS, List.of()));
        Assert.assertFalse(BulletGlassRules.isBreakableGlass(Material.STONE, List.of()));
    }

    @Test
    public void bulletsPassThroughIronBarsWithoutBreakingThem() {
        Assert.assertTrue(BulletGlassRules.isBulletPassThrough(Material.IRON_BARS));
        Assert.assertFalse(BulletGlassRules.isBulletPassThrough(Material.GLASS));
        Assert.assertFalse(BulletGlassRules.isBulletPassThrough(Material.STONE));
    }

    @Test
    public void configuredMaterialsOverrideDefaultPredicate() {
        Assert.assertTrue(BulletGlassRules.isBreakableGlass(Material.IRON_BARS, List.of("IRON_BARS")));
        Assert.assertFalse(BulletGlassRules.isBreakableGlass(Material.GLASS, List.of("IRON_BARS")));
    }

    @Test
    public void penetrationCountIsClamped() {
        Assert.assertEquals(0, BulletGlassRules.clampMaxPenetrations(-5));
        Assert.assertEquals(2, BulletGlassRules.clampMaxPenetrations(2));
        Assert.assertEquals(8, BulletGlassRules.clampMaxPenetrations(99));
    }
}
