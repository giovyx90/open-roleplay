package dev.openrp.vending.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class MachineLocationTest {

    @Test
    public void keyEncodesWorldAndCoordinates() {
        assertEquals("world:10:64:-20", new MachineLocation("world", 10, 64, -20).toKey());
    }

    @Test
    public void blankWorldDefaultsToWorld() {
        assertEquals("world", new MachineLocation("  ", 0, 0, 0).world());
    }

    @Test
    public void distanceToNullIsMax() {
        assertEquals(Double.MAX_VALUE, new MachineLocation("world", 0, 0, 0).distanceSquaredTo(null), 0.0);
    }

    @Test
    public void matchesBlockNullIsFalse() {
        assertFalse(new MachineLocation("world", 0, 0, 0).matchesBlock(null));
    }
}
