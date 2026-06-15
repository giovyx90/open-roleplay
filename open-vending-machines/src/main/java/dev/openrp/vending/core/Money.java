package dev.openrp.vending.core;

import java.util.Locale;

/** Tiny currency-formatting helper so prices render consistently (two decimals, dot separator). */
public final class Money {

    private Money() {
    }

    public static String format(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
