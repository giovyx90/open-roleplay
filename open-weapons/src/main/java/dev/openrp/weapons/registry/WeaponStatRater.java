package dev.openrp.weapons.registry;

import dev.openrp.weapons.model.WeaponCategory;

final class WeaponStatRater {

    private WeaponStatRater() {
    }

    static int scoreDamage(WeaponCategory category, double damage) {
        return scoreAscending(damage, switch (category) {
            case PISTOL -> new double[]{4.4D, 4.8D, 6.0D, 7.5D};
            case SMG -> new double[]{3.8D, 4.2D, 4.8D, 5.5D};
            case ASSAULT_RIFLE, SEMI_AUTO_RIFLE -> new double[]{6.0D, 6.4D, 7.0D, 7.6D};
            case SHOTGUN -> new double[]{10.0D, 14.0D, 18.0D, 22.0D};
            case SNIPER -> new double[]{12.0D, 17.0D, 26.0D, 34.0D};
            default -> new double[]{4.0D, 6.0D, 8.0D, 10.0D};
        });
    }

    static int scoreFireRate(WeaponCategory category, int fireRateTicks) {
        if (category == WeaponCategory.SHOTGUN || category == WeaponCategory.SNIPER) {
            return scoreLowerIsBetter(fireRateTicks, 16.0D, 24.0D, 32.0D, 40.0D);
        }
        return scoreLowerIsBetter(fireRateTicks, 2.0D, 3.0D, 5.0D, 7.0D);
    }

    static int scoreRange(WeaponCategory category, double range) {
        return scoreAscending(range, switch (category) {
            case PISTOL -> new double[]{30.0D, 40.0D, 50.0D, 65.0D};
            case SMG -> new double[]{35.0D, 50.0D, 65.0D, 80.0D};
            case ASSAULT_RIFLE, SEMI_AUTO_RIFLE -> new double[]{60.0D, 80.0D, 95.0D, 115.0D};
            case SHOTGUN -> new double[]{15.0D, 24.0D, 35.0D, 45.0D};
            case SNIPER -> new double[]{100.0D, 150.0D, 190.0D, 230.0D};
            default -> new double[]{20.0D, 40.0D, 60.0D, 80.0D};
        });
    }

    static int scoreAim(WeaponCategory category, double adsSpreadDeg) {
        if (category == WeaponCategory.SHOTGUN) {
            return scoreLowerIsBetter(adsSpreadDeg, 1.5D, 3.0D, 5.0D, 7.0D);
        }
        return scoreLowerIsBetter(adsSpreadDeg, 0.18D, 0.35D, 0.55D, 0.9D);
    }

    private static int scoreAscending(double value, double[] thresholds) {
        if (value <= 0.0D) {
            return 1;
        }
        int score = 1;
        for (double threshold : thresholds) {
            if (value >= threshold) {
                score++;
            }
        }
        return clamp(score);
    }

    private static int scoreLowerIsBetter(double value, double five, double four, double three, double two) {
        if (value <= 0.0D) {
            return 1;
        }
        if (value <= five) {
            return 5;
        }
        if (value <= four) {
            return 4;
        }
        if (value <= three) {
            return 3;
        }
        if (value <= two) {
            return 2;
        }
        return 1;
    }

    private static int clamp(int score) {
        return Math.max(1, Math.min(5, score));
    }
}
