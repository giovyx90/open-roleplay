package dev.openrp.weapons.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JumpRestrictionManager {
    public static final String REASON_HANDCUFF = "handcuff";
    public static final String REASON_GUN = "gun";
    public static final String REASON_GUN_SHOT = "gun_shot";
    public static final String REASON_GUN_HIT = "gun_hit";
    public static final String REASON_DOWNED = "downed";
    public static final String REASON_SHIELD = "shield";

    private static final double RESTRICTED_JUMP_STRENGTH = 0.0D;
    private static final double STALE_RESTRICTED_JUMP_STRENGTH = 0.001D;
    private static final double PLAYER_DEFAULT_JUMP_STRENGTH = 0.42D;

    private static final Map<UUID, Double> originalJumpStrength = new HashMap<>();
    private static final Map<UUID, Set<String>> activeReasons = new HashMap<>();

    private JumpRestrictionManager() {
    }

    public static void restrict(Player player, String reason) {
        if (player == null || reason == null || reason.isBlank()) {
            return;
        }

        AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpAttribute == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Set<String> reasons = activeReasons.computeIfAbsent(uuid, ignored -> new HashSet<>());

        if (!reasons.contains(reason) && !originalJumpStrength.containsKey(uuid)) {
            originalJumpStrength.put(uuid, originalValueFor(jumpAttribute));
        }

        reasons.add(reason);
        jumpAttribute.setBaseValue(RESTRICTED_JUMP_STRENGTH);
    }

    public static void release(Player player, String reason) {
        if (player == null || reason == null || reason.isBlank()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Set<String> reasons = activeReasons.get(uuid);
        if (reasons == null) {
            repairStale(player);
            return;
        }

        reasons.remove(reason);
        if (!reasons.isEmpty()) {
            return;
        }

        activeReasons.remove(uuid);

        AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        Double originalValue = originalJumpStrength.remove(uuid);

        restoreOrRepair(jumpAttribute, originalValue);
    }

    public static void clearAll(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        activeReasons.remove(uuid);

        AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        Double originalValue = originalJumpStrength.remove(uuid);
        restoreOrRepair(jumpAttribute, originalValue);
    }

    public static boolean repairStale(Player player) {
        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Set<String> reasons = activeReasons.get(uuid);
        if (reasons != null && !reasons.isEmpty()) {
            return false;
        }

        activeReasons.remove(uuid);
        AttributeInstance jumpAttribute = player.getAttribute(Attribute.JUMP_STRENGTH);
        Double originalValue = originalJumpStrength.remove(uuid);
        return restoreOrRepair(jumpAttribute, originalValue);
    }

    public static void onDownedStart(Player player) {
        restrict(player, REASON_DOWNED);
    }

    public static void onDownedEnd(Player player) {
        release(player, REASON_DOWNED);
    }

    public static Set<String> getActiveReasons(Player player) {
        if (player == null) {
            return Collections.emptySet();
        }
        Set<String> reasons = activeReasons.get(player.getUniqueId());
        if (reasons == null || reasons.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(reasons);
    }

    private static double originalValueFor(AttributeInstance jumpAttribute) {
        double value = jumpAttribute.getBaseValue();
        return isRestrictedValue(value) ? PLAYER_DEFAULT_JUMP_STRENGTH : value;
    }

    private static boolean restoreOrRepair(AttributeInstance jumpAttribute, Double originalValue) {
        if (jumpAttribute == null) {
            return false;
        }

        if (originalValue != null) {
            jumpAttribute.setBaseValue(isRestrictedValue(originalValue) ? PLAYER_DEFAULT_JUMP_STRENGTH : originalValue);
            return true;
        }

        if (isRestrictedValue(jumpAttribute.getBaseValue())) {
            jumpAttribute.setBaseValue(PLAYER_DEFAULT_JUMP_STRENGTH);
            return true;
        }

        return false;
    }

    private static boolean isRestrictedValue(double value) {
        return value <= STALE_RESTRICTED_JUMP_STRENGTH;
    }
}
