package dev.openrp.jobs.adapter;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Delivers user-facing notifications. The default sends chat messages and action bars. */
public interface NotificationAdapter {

    String id();

    void send(Player player, Component message);

    void actionBar(Player player, Component message);
}
