package dev.openrp.weapons.registry;

import dev.openrp.weapons.model.WeaponCategory;
import org.junit.Assert;
import org.junit.Test;

public class WeaponStatRaterTest {

    @Test
    public void pistolDamageUsesCategoryThresholdsInsteadOfGlobalMaximum() {
        Assert.assertTrue(WeaponStatRater.scoreDamage(WeaponCategory.PISTOL, 4.7D) > 1);
        Assert.assertTrue(WeaponStatRater.scoreDamage(WeaponCategory.PISTOL, 4.8D) >= 3);
    }

    @Test
    public void deagleLikePistolDamageScoresHigh() {
        Assert.assertEquals(5, WeaponStatRater.scoreDamage(WeaponCategory.PISTOL, 8.0D));
    }

    @Test
    public void sniperDamageKeepsBarrettAtMaximum() {
        Assert.assertEquals(5, WeaponStatRater.scoreDamage(WeaponCategory.SNIPER, 36.0D));
    }

    @Test
    public void fireRateUsesDifferentThresholdsForSlowCategories() {
        Assert.assertEquals(5, WeaponStatRater.scoreFireRate(WeaponCategory.SMG, 2));
        Assert.assertEquals(3, WeaponStatRater.scoreFireRate(WeaponCategory.SHOTGUN, 32));
    }

    @Test
    public void aimUsesShotgunSpecificSpreadThresholds() {
        Assert.assertEquals(3, WeaponStatRater.scoreAim(WeaponCategory.SHOTGUN, 4.5D));
        Assert.assertEquals(3, WeaponStatRater.scoreAim(WeaponCategory.PISTOL, 0.45D));
    }
}
