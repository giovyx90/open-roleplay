package dev.openrp.vending.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;
import dev.openrp.vending.model.VendingMachineState;

/**
 * Fired before a machine changes state (e.g. ACTIVE to EMPTY, or an admin DISABLE). Cancel to keep
 * the previous state. The machine still reports its old state until the change is applied.
 */
public class VendingMachineStateChangeEvent extends VendingMachineEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final VendingMachineState oldState;
    private final VendingMachineState newState;
    private boolean cancelled;

    public VendingMachineStateChangeEvent(VendingMachine machine, VendingMachineState oldState, VendingMachineState newState) {
        super(machine);
        this.oldState = oldState;
        this.newState = newState;
    }

    public VendingMachineState getOldState() {
        return oldState;
    }

    public VendingMachineState getNewState() {
        return newState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
