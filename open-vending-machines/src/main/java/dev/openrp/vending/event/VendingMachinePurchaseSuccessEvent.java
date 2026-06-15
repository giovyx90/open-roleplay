package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired after a completed sale: money has moved, the item has been delivered, stock reduced and the
 * cash box credited. Informational only (not cancellable).
 */
public class VendingMachinePurchaseSuccessEvent extends VendingMachineEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String productId;
    private final int amount;
    private final double totalPaid;

    public VendingMachinePurchaseSuccessEvent(Player player, VendingMachine machine, String productId, int amount, double totalPaid) {
        super(machine);
        this.player = player;
        this.productId = productId;
        this.amount = amount;
        this.totalPaid = totalPaid;
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

    public double getTotalPaid() {
        return totalPaid;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
