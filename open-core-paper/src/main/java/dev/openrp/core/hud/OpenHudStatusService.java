package dev.openrp.core.hud;

import dev.openrp.core.api.hud.OpenHudService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OpenHudStatusService implements OpenHudService {
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void show(Player player, Component message, long ttlTicks) {
        if (player == null || message == null || ttlTicks <= 0L) {
            return;
        }
        entries.put(player.getUniqueId(), new Entry(message, System.currentTimeMillis() + ttlTicks * 50L));
    }

    @Override
    public Component activeStatus(Player player) {
        if (player == null) {
            return null;
        }
        Entry entry = entries.get(player.getUniqueId());
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= System.currentTimeMillis()) {
            entries.remove(player.getUniqueId(), entry);
            return null;
        }
        return entry.message();
    }

    @Override
    public void clear(Player player) {
        if (player != null) {
            entries.remove(player.getUniqueId());
        }
    }

    @Override
    public void clearAll() {
        entries.clear();
    }

    private record Entry(Component message, long expiresAtMillis) {
    }
}
