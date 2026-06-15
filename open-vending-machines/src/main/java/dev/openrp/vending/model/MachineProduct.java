package dev.openrp.vending.model;

import java.util.Locale;

/**
 * A product as actually stocked inside one machine: it carries the live, mutable stock and the
 * (possibly overridden) price and capacity. All mutation goes through clamping setters so stock can
 * never go negative or exceed capacity. Mutation must be performed under the machine lock held by
 * the transaction services.
 */
public final class MachineProduct {

    private final String productId;
    private double price;
    private int stock;
    private int capacity;

    public MachineProduct(String productId, double price, int stock, int capacity) {
        this.productId = productId == null ? "" : productId.trim().toLowerCase(Locale.ROOT);
        this.capacity = Math.max(0, capacity);
        this.price = Math.max(0.0, price);
        this.stock = clampStock(stock, this.capacity);
    }

    public String productId() {
        return productId;
    }

    public double price() {
        return price;
    }

    public void setPrice(double newPrice) {
        this.price = Math.max(0.0, newPrice);
    }

    public int stock() {
        return stock;
    }

    public void setStock(int newStock) {
        this.stock = clampStock(newStock, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public void setCapacity(int newCapacity) {
        this.capacity = Math.max(0, newCapacity);
        this.stock = clampStock(stock, this.capacity);
    }

    public boolean inStock() {
        return stock > 0;
    }

    public boolean isFull() {
        return stock >= capacity;
    }

    /** Free space remaining before hitting capacity. */
    public int freeSpace() {
        return Math.max(0, capacity - stock);
    }

    /** Adds up to {@code amount}, never exceeding capacity; returns the quantity actually added. */
    public int addStock(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int added = Math.min(amount, freeSpace());
        this.stock += added;
        return added;
    }

    /** Removes up to {@code amount}, never below zero; returns the quantity actually removed. */
    public int removeStock(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int removed = Math.min(amount, stock);
        this.stock -= removed;
        return removed;
    }

    public static int clampStock(int value, int capacity) {
        return Math.max(0, Math.min(value, Math.max(0, capacity)));
    }
}
