package dev.openrp.vending.core;

/** Outcome of a restock attempt. */
public record RestockResult(boolean success, String productId, int amount, int newStock, int capacity) {

    public static RestockResult ok(String productId, int amount, int newStock, int capacity) {
        return new RestockResult(true, productId, amount, newStock, capacity);
    }

    public static RestockResult fail(String productId) {
        return new RestockResult(false, productId, 0, 0, 0);
    }
}
