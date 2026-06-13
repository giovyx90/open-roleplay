package dev.openrp.weapons.api;

public final class WeaponCombatDecision {
    private static final WeaponCombatDecision ALLOW = new WeaponCombatDecision(true, null);

    private final boolean allowed;
    private final String feedback;

    private WeaponCombatDecision(boolean allowed, String feedback) {
        this.allowed = allowed;
        this.feedback = feedback;
    }

    public static WeaponCombatDecision allow() {
        return ALLOW;
    }

    public static WeaponCombatDecision deny() {
        return deny(null);
    }

    public static WeaponCombatDecision deny(String feedback) {
        return new WeaponCombatDecision(false, feedback);
    }

    public static WeaponCombatDecision merge(WeaponCombatDecision first, WeaponCombatDecision second) {
        if (first != null && !first.isAllowed()) {
            return first;
        }
        if (second != null && !second.isAllowed()) {
            return second;
        }
        return allow();
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isDenied() {
        return !allowed;
    }

    public String feedback() {
        return feedback;
    }
}
