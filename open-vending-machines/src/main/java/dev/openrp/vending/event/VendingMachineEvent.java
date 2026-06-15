package dev.openrp.vending.event;

import org.bukkit.event.Event;
import dev.openrp.vending.model.VendingMachine;

/**
 * Base class for every Open Vending Machines event. Carries the affected machine; each concrete
 * subclass keeps its own {@code HandlerList} as Bukkit requires.
 */
public abstract class VendingMachineEvent extends Event {

    private final VendingMachine machine;

    protected VendingMachineEvent(VendingMachine machine) {
        this.machine = machine;
    }

    public VendingMachine getMachine() {
        return machine;
    }
}
