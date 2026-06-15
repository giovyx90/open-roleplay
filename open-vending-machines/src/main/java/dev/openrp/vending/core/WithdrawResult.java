package dev.openrp.vending.core;

/** Outcome of a cash withdrawal. {@code destinationAccount} is {@code null} when paid to the player. */
public record WithdrawResult(boolean success, double amount, String destinationAccount) {

    public static WithdrawResult ok(double amount, String destinationAccount) {
        return new WithdrawResult(true, amount, destinationAccount);
    }

    public static WithdrawResult fail() {
        return new WithdrawResult(false, 0.0, null);
    }
}
