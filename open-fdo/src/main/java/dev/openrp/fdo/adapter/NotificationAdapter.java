package dev.openrp.fdo.adapter;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Delivers a notice to a player (escape alerts, custody warnings, alert-state broadcasts). */
public interface NotificationAdapter {

    String id();

    void notify(Player player, Component message);
}
