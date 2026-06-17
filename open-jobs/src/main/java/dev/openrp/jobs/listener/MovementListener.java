package dev.openrp.jobs.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import dev.openrp.jobs.OpenJobsPlugin;

/**
 * Drives session pause/resume on region boundaries. A session pauses when its worker leaves the
 * location region (clock stopped) and resumes on return; logging out pauses rather than ends, so a
 * brief disconnection is not punished. Movement is only re-evaluated on a block change to stay cheap.
 */
public final class MovementListener implements Listener {

    private final OpenJobsPlugin plugin;

    public MovementListener(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (changedBlock(event.getFrom(), event.getTo()) && plugin.sessions().byPlayer(event.getPlayer().getUniqueId()).isPresent()) {
            plugin.sessions().refreshPresence(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.sessions().byPlayer(event.getPlayer().getUniqueId()).isPresent()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.sessions().refreshPresence(event.getPlayer()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.sessions().byPlayer(event.getPlayer().getUniqueId()).isPresent()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.sessions().refreshPresence(event.getPlayer()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.sessions().pauseForQuit(event.getPlayer());
    }

    private static boolean changedBlock(Location from, Location to) {
        if (to == null) {
            return false;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
