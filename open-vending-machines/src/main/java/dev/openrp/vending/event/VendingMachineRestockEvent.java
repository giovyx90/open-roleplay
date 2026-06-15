package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired before stock is added to a machine. Cancel to block the refill. {@link #getAmount()} is the
 * requested amount; the core clamps it to capacity and to the available source items afterwards.
 */
public class VendingMachineRestockEvent extends VendingMachineEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String productId;
    private final int amount;
    private boolean cancelled;

    public VendingMachineRestockEvent(Player player, VendingMachine machine, String productId, int amount) {
        super(machine);
        this.player = player;
        this.productId = productId;
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public String getProductId() {
        return productId;
    }

    public int getAmount() {
        return amount;
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
