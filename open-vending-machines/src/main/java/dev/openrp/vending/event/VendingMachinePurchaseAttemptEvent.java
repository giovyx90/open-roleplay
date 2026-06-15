package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired after server-side validation passes but before money and items move. Cancel to veto the
 * sale (the buyer receives a generic "denied" failure). The unit price is already resolved through
 * hooks, so it reflects the price the buyer would actually pay.
 */
public class VendingMachinePurchaseAttemptEvent extends VendingMachineEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String productId;
    private final int amount;
    private final double unitPrice;
    private boolean cancelled;

    public VendingMachinePurchaseAttemptEvent(Player player, VendingMachine machine, String productId, int amount, double unitPrice) {
        super(machine);
        this.player = player;
        this.productId = productId;
        this.amount = amount;
        this.unitPrice = unitPrice;
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

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalPrice() {
        return unitPrice * amount;
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
