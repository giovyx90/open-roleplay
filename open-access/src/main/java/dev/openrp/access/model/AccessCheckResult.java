package dev.openrp.access.model;

public record AccessCheckResult(AccessDecision decision, String reason) {

    public static AccessCheckResult allow(String reason) {
        return new AccessCheckResult(AccessDecision.ALLOW, reason);
    }

    public static AccessCheckResult deny(String reason) {
        return new AccessCheckResult(AccessDecision.DENY, reason);
    }

    public static AccessCheckResult pass(String reason) {
        return new AccessCheckResult(AccessDecision.PASS, reason);
    }

    public boolean denied() {
        return decision == AccessDecision.DENY;
    }
}
