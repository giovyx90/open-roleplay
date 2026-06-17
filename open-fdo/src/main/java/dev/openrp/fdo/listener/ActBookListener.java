package dev.openrp.fdo.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import dev.openrp.fdo.OpenFdoPlugin;

/**
 * Turns the signing of a tagged act book into a filed act. When a member signs a writable book that
 * was issued by {@code /atto}, this stamps the resulting written book and applies the act's effects;
 * ordinary books are left untouched. The book content is whatever the member wrote - the plugin only
 * stamps and registers.
 */
public final class ActBookListener implements Listener {

    private final OpenFdoPlugin plugin;

    public ActBookListener(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    // HIGHEST + ignoreCancelled so we run after plugins that might veto a book edit: the act's
    // effects must not be committed for a signing another plugin then cancels. The PDC tags are
    // read from getPreviousBookMeta() (the writable book's meta, which carries the container) rather
    // than indexing the inventory by slot - signing from the OFF-HAND reports getSlot() == -1, and
    // getItem(-1) would throw.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEditBook(PlayerEditBookEvent event) {
        if (!event.isSigning()) {
            return;
        }
        BookMeta previous = event.getPreviousBookMeta();
        if (!plugin.actBook().isActBook(previous)) {
            return;
        }
        BookMeta newMeta = event.getNewBookMeta();
        if (plugin.acts().completeAct(event.getPlayer(), previous, newMeta)) {
            event.setNewBookMeta(newMeta);
        }
    }
}
