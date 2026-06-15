package dev.openrp.vending.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.vending.model.VendingMachine;

/**
 * Fired before the internal cash box is emptied into a destination account. Cancel to keep the cash
 * in the machine. {@link #getDestinationAccount()} is the resolved target (a company account when
 * the machine has an owner and company deposits are enabled, otherwise the withdrawing player).
 */
public class VendingMachineCashWithdrawEvent extends VendingMachineEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final double amount;
    private final String destinationAccount;
    private boolean cancelled;

    public VendingMachineCashWithdrawEvent(Player player, VendingMachine machine, double amount, String destinationAccount) {
        super(machine);
        this.player = player;
        this.amount = amount;
        this.destinationAccount = destinationAccount;
    }

    public Player getPlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

    /** Resolved destination: a company account id, or {@code null} to pay the player directly. */
    public String getDestinationAccount() {
        return destinationAccount;
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
