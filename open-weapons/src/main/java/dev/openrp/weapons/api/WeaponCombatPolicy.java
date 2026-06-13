package dev.openrp.weapons.api;

public interface WeaponCombatPolicy {
    default WeaponCombatDecision canUse(WeaponUseContext context) {
        return WeaponCombatDecision.allow();
    }

    default WeaponCombatDecision canTarget(WeaponTargetContext context) {
        return WeaponCombatDecision.allow();
    }

    default WeaponCombatDecision beforeImpact(WeaponImpactContext context) {
        return WeaponCombatDecision.allow();
    }

    default void onShot(WeaponUseContext context) {
    }

    default void onHit(WeaponImpactContext context) {
    }
}
