package dev.openrp.weapons.bridge;

import org.bukkit.entity.Player;

public final class OpenBankBridge {
    public boolean isAvailable() {
        return false;
    }

    public void pay(Player player, double amount, String accountName, PaymentCallback callback) {
        if (callback != null) {
            callback.failure("Il sistema bancario non e' disponibile.");
        }
    }

    @FunctionalInterface
    public interface PaymentCallback {
        void failure(String message);
    }
}
