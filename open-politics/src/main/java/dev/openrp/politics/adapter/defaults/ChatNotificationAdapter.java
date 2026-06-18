package dev.openrp.politics.adapter.defaults;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import dev.openrp.politics.adapter.NotificationAdapter;

/** Default notification adapter: chat to a player, server broadcast for public announcements. */
public final class ChatNotificationAdapter implements NotificationAdapter {

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public void notifyPlayer(Player player, Component message) {
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    @Override
    public void broadcast(Component message) {
        Bukkit.getServer().broadcast(message);
    }
}
