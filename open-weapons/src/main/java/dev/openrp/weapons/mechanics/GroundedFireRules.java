package dev.openrp.weapons.mechanics;

public final class GroundedFireRules {
    public static final double MAX_STABLE_VERTICAL_VELOCITY = 0.08D;
    public static final float MAX_STABLE_FALL_DISTANCE = 0.20F;

    private GroundedFireRules() {
    }

    public static boolean canFire(boolean onGround, double verticalVelocity, float fallDistance, boolean supportBelow) {
        if (onGround) {
            return true;
        }
        return supportBelow
                && Math.abs(verticalVelocity) <= MAX_STABLE_VERTICAL_VELOCITY
                && fallDistance <= MAX_STABLE_FALL_DISTANCE;
    }
}
