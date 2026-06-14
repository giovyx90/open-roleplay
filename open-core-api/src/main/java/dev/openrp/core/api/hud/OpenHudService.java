package dev.openrp.core.api.hud;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface OpenHudService {
    void show(Player player, Component message, long ttlTicks);

    Component activeStatus(Player player);

    void clear(Player player);

    void clearAll();
}
