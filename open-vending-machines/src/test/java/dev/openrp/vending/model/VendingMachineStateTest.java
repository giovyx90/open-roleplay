package dev.openrp.vending.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VendingMachineStateTest {

    @Test
    public void onlyActiveCanSell() {
        assertTrue(VendingMachineState.ACTIVE.canSell());
        assertFalse(VendingMachineState.EMPTY.canSell());
        assertFalse(VendingMachineState.BROKEN.canSell());
        assertFalse(VendingMachineState.DISABLED.canSell());
    }

    @Test
    public void fromStringIsLenient() {
        assertEquals(VendingMachineState.BROKEN, VendingMachineState.fromString("broken"));
        assertEquals(VendingMachineState.DISABLED, VendingMachineState.fromString("DISABLED"));
        assertEquals(VendingMachineState.ACTIVE, VendingMachineState.fromString("nonsense"));
        assertEquals(VendingMachineState.ACTIVE, VendingMachineState.fromString(null));
    }
}
