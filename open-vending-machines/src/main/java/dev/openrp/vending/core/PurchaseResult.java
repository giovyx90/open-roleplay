package dev.openrp.vending.core;

import dev.openrp.vending.event.PurchaseFailReason;

/** Outcome of a purchase attempt, returned to programmatic callers and the GUI. */
public record PurchaseResult(boolean success, PurchaseFailReason reason, String productId, int amount, double totalPaid) {

    public static PurchaseResult ok(String productId, int amount, double totalPaid) {
        return new PurchaseResult(true, null, productId, amount, totalPaid);
    }

    public static PurchaseResult fail(PurchaseFailReason reason, String productId) {
        return new PurchaseResult(false, reason, productId, 0, 0.0);
    }
}
