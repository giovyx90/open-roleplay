package dev.openrp.vending.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class VendingMachineTest {

    private VendingMachine machine(String owner) {
        return new VendingMachine(UUID.randomUUID(), "snack", new MachineLocation("world", 1, 2, 3), owner);
    }

    @Test
    public void outOfStockWhenAllProductsZero() {
        VendingMachine machine = machine(null);
        machine.putProduct(new MachineProduct("a", 1.0, 0, 10));
        machine.putProduct(new MachineProduct("b", 1.0, 0, 10));
        assertTrue(machine.isOutOfStock());
        machine.product("a").setStock(2);
        assertFalse(machine.isOutOfStock());
    }

    @Test
    public void refreshPreservesBrokenAndDisabled() {
        VendingMachine machine = machine(null);
        machine.putProduct(new MachineProduct("a", 1.0, 0, 10));
        machine.setState(VendingMachineState.BROKEN);
        assertEquals(VendingMachineState.BROKEN, machine.refreshAutomaticState());
        machine.setState(VendingMachineState.DISABLED);
        assertEquals(VendingMachineState.DISABLED, machine.refreshAutomaticState());
    }

    @Test
    public void refreshTogglesActiveAndEmpty() {
        VendingMachine machine = machine(null);
        machine.putProduct(new MachineProduct("a", 1.0, 0, 10));
        assertEquals(VendingMachineState.EMPTY, machine.refreshAutomaticState());
        machine.product("a").setStock(5);
        assertEquals(VendingMachineState.ACTIVE, machine.refreshAutomaticState());
    }

    @Test
    public void cashDepositAndDrain() {
        VendingMachine machine = machine(null);
        machine.depositCash(10.5);
        machine.depositCash(4.5);
        assertEquals(15.0, machine.cashBalance(), 1e-9);
        assertEquals(15.0, machine.drainCash(), 1e-9);
        assertEquals(0.0, machine.cashBalance(), 1e-9);
    }

    @Test
    public void ownerHandlingTreatsBlankAsNone() {
        VendingMachine owned = machine("redspot");
        assertTrue(owned.hasOwner());
        assertEquals("redspot", owned.ownerCompanyId().orElse(null));
        owned.setOwnerCompanyId("   ");
        assertFalse(owned.hasOwner());
    }

    @Test
    public void shortIdIsEightCharacters() {
        assertEquals(8, machine(null).shortId().length());
    }
}
