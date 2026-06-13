package dev.openrp.weapons.mechanics;

import org.junit.Assert;
import org.junit.Test;

public class GroundedFireRulesTest {
    @Test
    public void bukkitGroundedAlwaysAllowsFire() {
        Assert.assertTrue(GroundedFireRules.canFire(true, -0.6D, 4.0F, false));
    }

    @Test
    public void stableSupportedPlayerCanFireDuringGroundDesync() {
        Assert.assertTrue(GroundedFireRules.canFire(false, 0.02D, 0.0F, true));
    }

    @Test
    public void fallingPlayerCannotFire() {
        Assert.assertFalse(GroundedFireRules.canFire(false, -0.32D, 1.1F, true));
        Assert.assertFalse(GroundedFireRules.canFire(false, 0.0D, 0.0F, false));
    }
}
