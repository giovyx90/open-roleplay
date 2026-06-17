package dev.openrp.fdo.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.config.ActDefinition;
import dev.openrp.fdo.model.Agent;

/**
 * The {@code /atto nuovo} picker: a chest menu listing only the acts the member may currently produce
 * (rank capability present AND any required adapter registered), exactly mirroring the design's rule.
 * Clicking an act issues its blank book through the same validated path as the command.
 */
public final class ActMenu {

    private final OpenFdoPlugin plugin;

    public ActMenu(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Agent agent) {
        List<ActDefinition> available = plugin.acts().available(agent);
        if (available.isEmpty()) {
            plugin.messages().warning(player, "act.none_available");
            return;
        }
        int size = Math.min(54, Math.max(9, ((available.size() - 1) / 9 + 1) * 9));
        ActMenuHolder holder = new ActMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, size,
                plugin.messages().mini(player, "act.menu_title"));
        holder.setInventory(inventory);

        int slot = 0;
        for (ActDefinition act : available) {
            if (slot >= size) {
                break;
            }
            inventory.setItem(slot, icon(act));
            holder.bind(slot, act.id());
            slot++;
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(ActDefinition act) {
        Material material = Material.matchMaterial(act.icon());
        ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(act.displayName(), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(act.capability().name(), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
