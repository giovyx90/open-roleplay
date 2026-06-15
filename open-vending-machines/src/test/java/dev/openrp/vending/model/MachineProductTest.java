package dev.openrp.vending.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MachineProductTest {

    @Test
    public void clampsStockWithinCapacity() {
        assertEquals(0, MachineProduct.clampStock(-5, 10));
        assertEquals(7, MachineProduct.clampStock(7, 10));
        assertEquals(10, MachineProduct.clampStock(15, 10));
        assertEquals(0, MachineProduct.clampStock(5, 0));
    }

    @Test
    public void addStockNeverExceedsCapacity() {
        MachineProduct product = new MachineProduct("chips", 2.0, 5, 10);
        assertEquals(5, product.addStock(20));
        assertEquals(10, product.stock());
        assertTrue(product.isFull());
        assertEquals(0, product.addStock(3));
    }

    @Test
    public void removeStockNeverBelowZero() {
        MachineProduct product = new MachineProduct("chips", 2.0, 3, 10);
        assertEquals(3, product.removeStock(10));
        assertEquals(0, product.stock());
        assertFalse(product.inStock());
    }

    @Test
    public void setCapacityReclampsStock() {
        MachineProduct product = new MachineProduct("chips", 2.0, 8, 10);
        product.setCapacity(5);
        assertEquals(5, product.stock());
        assertEquals(5, product.capacity());
    }

    @Test
    public void freeSpaceReflectsRemainingCapacity() {
        MachineProduct product = new MachineProduct("x", 1.0, 4, 10);
        assertEquals(6, product.freeSpace());
    }

    @Test
    public void negativePriceClampedToZero() {
        MachineProduct product = new MachineProduct("x", -3.0, 0, 5);
        assertEquals(0.0, product.price(), 1e-9);
        product.setPrice(-1.0);
        assertEquals(0.0, product.price(), 1e-9);
    }
}
