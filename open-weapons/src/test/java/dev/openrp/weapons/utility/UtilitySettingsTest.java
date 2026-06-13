package dev.openrp.weapons.utility;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class UtilitySettingsTest {

    @Test
    public void defaultsMatchGameplayPlan() {
        UtilitySettings settings = UtilitySettings.defaults();

        Assert.assertEquals(3, settings.scannerDurationSeconds());
        Assert.assertEquals(5, settings.handcuffDurationSeconds());
        Assert.assertEquals(3, settings.restraintDurationSeconds());
        Assert.assertEquals(5, settings.gagUses());
        Assert.assertEquals(5, settings.blindfoldUses());
        Assert.assertEquals(5, settings.fireAxeCooldownSeconds());
        Assert.assertEquals(131, settings.fireAxeUses());
        Assert.assertEquals(Material.PAPER, settings.fireAxeMaterial());
        Assert.assertEquals(5, settings.fireAxeDoorOpenSeconds());
        Assert.assertEquals(20.0D, settings.grapplingHookMaxDistance(), 0.01D);
        Assert.assertEquals(3, settings.grapplingHookCooldownSeconds());
        Assert.assertFalse(settings.trackerShowCoordinates());
        Assert.assertTrue(settings.trackerShowRegion());
        Assert.assertEquals(2.9D, settings.statusTagYOffset(), 0.01D);
    }

    @Test
    public void configOverridesValidValuesAndFallsBackForInvalidMaterial() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("scanner-duration-seconds", 4);
        config.set("fire-axe-material", "NOT_A_REAL_AXE");
        config.set("tracker-show-coordinates", true);

        UtilitySettings settings = UtilitySettings.fromConfig(config);

        Assert.assertEquals(4, settings.scannerDurationSeconds());
        Assert.assertEquals(Material.PAPER, settings.fireAxeMaterial());
        Assert.assertTrue(settings.trackerShowCoordinates());
    }
}
