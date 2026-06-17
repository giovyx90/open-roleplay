package dev.openrp.jobs.model;

import java.util.Locale;

/**
 * How a location type detects work. The core listens to the matching Bukkit event and only counts
 * actions on configured {@code valid_materials}: breaking dirt in a mine is not work, breaking stone is.
 * {@code BLOCK_BREAK}, {@code FISHING} and {@code CRAFTING} are tracked natively; {@code CHEST_DEPOSIT}
 * and {@code CUSTOM} are recognised for setting-neutral configs and left to server-specific bridges.
 */
public enum ActivityDetection {

    BLOCK_BREAK,
    FISHING,
    CRAFTING,
    CHEST_DEPOSIT,
    CUSTOM;

    public static ActivityDetection fromString(String raw) {
        if (raw == null) {
            return BLOCK_BREAK;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "fishing" -> FISHING;
            case "crafting" -> CRAFTING;
            case "chest_deposit", "deposit" -> CHEST_DEPOSIT;
            case "custom" -> CUSTOM;
            default -> BLOCK_BREAK;
        };
    }
}
