package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired when a purchase is rejected, carrying a typed {@link PurchaseFailReason}. Informational only
 * (not cancellable) - the sale has already been stopped. {@link #getProductId()} may be {@code null}
 * when the failure is not product-specific.
 */
public class VendingMachinePurchaseFailEvent extends VendingMachineEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String productId;
    private final PurchaseFailReason reason;

    public VendingMachinePurchaseFailEvent(Player player, VendingMachine machine, String productId, PurchaseFailReason reason) {
        super(machine);
        this.player = player;
        this.productId = productId;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public String getProductId() {
        return productId;
    }

    public PurchaseFailReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
