package dev.openrp.jobs.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import dev.openrp.jobs.OpenJobsPlugin;

/**
 * Feeds physical work events into the session tracker. Only counts toward a session that is currently
 * active in the right location type on a valid material - the manager enforces that. The plugin never
 * cancels or alters the events; mining, fishing and crafting stay normal Minecraft, they just may or
 * may not be paid work.
 */
public final class ActivityListener implements Listener {

    private final OpenJobsPlugin plugin;

    public ActivityListener(OpenJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        plugin.sessions().recordBlockBreak(event.getPlayer(), block.getType(),
                block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (event.getCaught() instanceof Item item) {
            plugin.sessions().recordFishing(event.getPlayer(), item.getItemStack().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : event.getCurrentItem();
        plugin.sessions().recordTransformation(player, result);
    }
}
