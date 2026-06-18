package dev.openrp.politics.adapter;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Delivers out-of-band notices (an opening election, a promulgated law, an appointment). The default
 * sends chat and a server-wide broadcast; a richer bridge could route to a discord or a HUD.
 */
public interface NotificationAdapter {

    String id();

    void notifyPlayer(Player player, Component message);

    /** Broadcast a public political announcement to the whole server. */
    void broadcast(Component message);
}
