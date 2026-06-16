package dev.openrp.companies.adapter;

import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Delivery channel for user-facing notifications (chat, action bar, toast, Discord relay, ...). The
 * core decides <em>who</em> should be told (e.g. online members of a company) and hands the
 * recipients to {@link #broadcast}, keeping this adapter free of any company-system coupling.
 */
public interface NotificationAdapter {

    String id();

    void notify(Player player, Component message);

    default void broadcast(Collection<? extends Player> recipients, Component message) {
        for (Player player : recipients) {
            notify(player, message);
        }
    }
}
