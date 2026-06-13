package dev.openrp.weapons.mechanics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatStunManager {
    private final Map<UUID, Long> stunnedPlayers = new ConcurrentHashMap<>();

    public void stun(UUID playerId, long durationMs) {
        long stunUntil = System.currentTimeMillis() + durationMs;
        stunnedPlayers.put(playerId, stunUntil);
    }

    public boolean isStunned(UUID playerId) {
        Long stunUntil = stunnedPlayers.get(playerId);
        if (stunUntil == null) return false;
        
        if (System.currentTimeMillis() > stunUntil) {
            stunnedPlayers.remove(playerId);
            return false;
        }
        
        return true;
    }

    public long getRemainingStunTimeSeconds(UUID playerId) {
        Long stunUntil = stunnedPlayers.get(playerId);
        if (stunUntil == null) return 0;
        
        long remaining = stunUntil - System.currentTimeMillis();
        if (remaining <= 0) {
            stunnedPlayers.remove(playerId);
            return 0;
        }
        
        return remaining / 1000;
    }
}
