package dev.openrp.companies.adapter.defaults;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import dev.openrp.companies.adapter.NotificationAdapter;

/** Default notification adapter: sends Adventure components to the player's chat. */
public final class ChatNotificationAdapter implements NotificationAdapter {

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public void notify(Player player, Component message) {
        if (player != null && message != null) {
            player.sendMessage(message);
        }
    }
}
