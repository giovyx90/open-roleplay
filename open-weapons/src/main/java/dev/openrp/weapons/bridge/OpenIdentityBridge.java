package dev.openrp.weapons.bridge;

import org.bukkit.entity.Player;

import java.util.UUID;

public final class OpenIdentityBridge {
    public void applyAnonymous(Player player) {
        if (player != null) {
            player.setCustomName("Non identificato");
            player.setCustomNameVisible(true);
        }
    }

    public void refreshPlayer(Player player) {
        if (player != null) {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }
    }

    public String displayName(Player player) {
        return player == null ? "" : player.getName();
    }

    public String displayName(UUID uuid, String fallback) {
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        return player == null ? fallback : player.getName();
    }
}
