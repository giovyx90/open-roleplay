package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired before a machine is removed and deleted from storage. Cancel to keep it.
 * {@link #getPlayer()} is {@code null} when removed programmatically via the API.
 */
public class VendingMachineRemoveEvent extends VendingMachineEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private boolean cancelled;

    public VendingMachineRemoveEvent(Player player, VendingMachine machine) {
        super(machine);
        this.player = player;
    }

    /** The removing player, or {@code null} if removed via the API. */
    public Player getPlayer() {
        return player;
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
