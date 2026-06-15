package dev.openrp.vending.hook;

/**
 * Allow/deny outcome returned by gate-style hooks. A deny carries a human-readable reason that the
 * core surfaces to the player (so integrators can explain <em>why</em> an action was blocked).
 */
public record VendingDecision(boolean allowed, String reason) {

    private static final VendingDecision ALLOW = new VendingDecision(true, "");

    public VendingDecision {
        reason = reason == null ? "" : reason;
    }

    public static VendingDecision allow() {
        return ALLOW;
    }

    public static VendingDecision deny(String reason) {
        return new VendingDecision(false, reason == null ? "" : reason);
    }

    public boolean denied() {
        return !allowed;
    }
}
