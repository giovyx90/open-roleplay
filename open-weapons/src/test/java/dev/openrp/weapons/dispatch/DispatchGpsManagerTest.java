package dev.openrp.weapons.dispatch;

import org.bukkit.Location;
import org.junit.Assert;
import org.junit.Test;

public class DispatchGpsManagerTest {

    @Test
    public void directionLabelUsesPlainEnglishText() {
        Assert.assertEquals("Ahead", DispatchGpsManager.directionLabel(0.0F, 0.0, 10.0));
        Assert.assertEquals("Behind", DispatchGpsManager.directionLabel(0.0F, 0.0, -10.0));
    }

    @Test
    public void directionIndexWrapsAroundYaw() {
        Assert.assertEquals(0, DispatchGpsManager.directionIndex(359.0F, 0.0, 10.0));
        Assert.assertEquals(0, DispatchGpsManager.directionIndex(-1.0F, 0.0, 10.0));
    }

    @Test
    public void gpsTextCanHideCoordinates() {
        DispatchGpsManager gps = new DispatchGpsManager(null);
        Location from = new Location(null, 0.0, 64.0, 0.0, 0.0F, 0.0F);
        Location to = new Location(null, 0.0, 70.0, 25.0);

        Assert.assertEquals("Ahead | TRACKER | 25m", gps.formatGpsText(from, to, "TRACKER", 25.0, false));
    }
}
