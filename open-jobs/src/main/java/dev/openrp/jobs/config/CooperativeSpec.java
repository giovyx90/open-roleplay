package dev.openrp.jobs.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * The cooperative bonus: when several workers of the same job work the same location at once, every
 * participant earns a multiplier. No command is needed - the plugin detects the concurrent sessions
 * and applies it automatically; leaving the region drops the bonus immediately. Working alone is always
 * possible, working together is simply more profitable.
 */
public final class CooperativeSpec {

    private final boolean enabled;
    private final int minPlayers;
    private final int maxBonusPlayers;
    private final double bonusPerPlayer;

    public CooperativeSpec(boolean enabled, int minPlayers, int maxBonusPlayers, double bonusPerPlayer) {
        this.enabled = enabled;
        this.minPlayers = Math.max(2, minPlayers);
        this.maxBonusPlayers = Math.max(this.minPlayers, maxBonusPlayers);
        this.bonusPerPlayer = Math.max(0.0, bonusPerPlayer);
    }

    public static CooperativeSpec disabled() {
        return new CooperativeSpec(false, 2, 4, 0.05);
    }

    public static CooperativeSpec from(ConfigurationSection section) {
        if (section == null) {
            return disabled();
        }
        return new CooperativeSpec(
                section.getBoolean("enabled", false),
                section.getInt("min_players", 2),
                section.getInt("max_bonus_players", 4),
                section.getDouble("bonus_per_player", 0.05));
    }

    public boolean enabled() {
        return enabled;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public int maxBonusPlayers() {
        return maxBonusPlayers;
    }

    public double bonusPerPlayer() {
        return bonusPerPlayer;
    }

    /**
     * The multiplier for a group of {@code participants} concurrent workers. Below {@code minPlayers}
     * there is no bonus (1.0); above {@code maxBonusPlayers} the bonus stops scaling.
     */
    public double multiplierFor(int participants) {
        if (!enabled || participants < minPlayers) {
            return 1.0;
        }
        int counted = Math.min(participants, maxBonusPlayers);
        return 1.0 + (counted - 1) * bonusPerPlayer;
    }
}
