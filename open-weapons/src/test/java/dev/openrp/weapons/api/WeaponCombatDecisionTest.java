package dev.openrp.weapons.api;

import org.junit.Assert;
import org.junit.Test;

public class WeaponCombatDecisionTest {

    @Test
    public void mergeKeepsAllowWhenBothPoliciesAllow() {
        WeaponCombatDecision merged = WeaponCombatDecision.merge(
                WeaponCombatDecision.allow(),
                WeaponCombatDecision.allow());

        Assert.assertTrue(merged.isAllowed());
    }

    @Test
    public void mergeKeepsFirstDenyFeedback() {
        WeaponCombatDecision merged = WeaponCombatDecision.merge(
                WeaponCombatDecision.deny("round closed"),
                WeaponCombatDecision.deny("friendly fire"));

        Assert.assertTrue(merged.isDenied());
        Assert.assertEquals("round closed", merged.feedback());
    }

    @Test
    public void mergeUsesSecondDenyWhenFirstAllows() {
        WeaponCombatDecision merged = WeaponCombatDecision.merge(
                WeaponCombatDecision.allow(),
                WeaponCombatDecision.deny("friendly fire"));

        Assert.assertTrue(merged.isDenied());
        Assert.assertEquals("friendly fire", merged.feedback());
    }
}
