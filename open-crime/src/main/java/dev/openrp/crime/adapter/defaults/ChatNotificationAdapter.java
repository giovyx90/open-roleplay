package dev.openrp.crime.adapter.defaults;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import dev.openrp.crime.adapter.NotificationAdapter;

/** Default notification adapter: chat messages and action bars straight to the player. */
public final class ChatNotificationAdapter implements NotificationAdapter {

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public void send(Player player, Component message) {
        if (player != null && message != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public void actionBar(Player player, Component message) {
        if (player != null && message != null) {
            player.sendActionBar(message);
        }
    }
}
