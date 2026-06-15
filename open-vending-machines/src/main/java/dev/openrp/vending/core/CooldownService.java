package dev.openrp.vending.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player anti-spam cooldown for purchases. */
public final class CooldownService {

    private final Map<UUID, Long> lastAction = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID playerId, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return false;
        }
        Long last = lastAction.get(playerId);
        return last != null && (System.currentTimeMillis() - last) < cooldownMillis;
    }

    public void mark(UUID playerId) {
        lastAction.put(playerId, System.currentTimeMillis());
    }

    public void clear(UUID playerId) {
        lastAction.remove(playerId);
    }
}
