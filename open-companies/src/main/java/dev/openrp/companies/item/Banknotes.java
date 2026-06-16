package dev.openrp.companies.item;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import dev.openrp.companies.OpenCompaniesPlugin;

/**
 * Physical money. A banknote is an {@link ItemStack} tagged with its face value in its
 * {@link PersistentDataContainer}; the "cash" a player holds is simply the sum of the banknotes in
 * their inventory - there is no hidden cash balance. This class creates banknotes, counts them, and
 * implements taking an exact amount (handing back change) and dispensing an amount, reusing the pure
 * {@link Denominations} arithmetic for the breakdown.
 *
 * <p>Denomination model uses one base {@link Material} (config {@code finance.currency.banknote-material})
 * with the face value as {@code CustomModelData}, so a resource pack can render a distinct model per
 * value while the server treats them uniformly.</p>
 */
public final class Banknotes {

    private final OpenCompaniesPlugin plugin;
    private final NamespacedKey valueKey;
    private final Material material;

    public Banknotes(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
        this.valueKey = new NamespacedKey(plugin, "banknote_value");
        Material parsed = Material.matchMaterial(plugin.settings().banknoteMaterial());
        this.material = parsed == null ? Material.PAPER : parsed;
    }

    /** A stack of {@code count} banknotes of the given face value. */
    public ItemStack create(int value, int count) {
        ItemStack item = new ItemStack(material, Math.max(1, count));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(plugin.settings().currencySymbol() + value, NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setCustomModelData(value);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.INTEGER, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Face value of a single banknote item, or {@code 0} if it is not a banknote. */
    public int valueOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return 0;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer()
                .get(valueKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    public boolean isBanknote(ItemStack item) {
        return valueOf(item) > 0;
    }

    /** Total face value of every banknote in the player's inventory. */
    public long cashOnHand(Player player) {
        long total = 0L;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            total += (long) valueOf(item) * (item == null ? 0 : item.getAmount());
        }
        return total;
    }

    /**
     * Removes exactly {@code amount} of cash from the player, handing back change if their notes
     * overpaid. Returns {@code false} (taking nothing) if they do not hold enough cash.
     */
    public boolean take(Player player, long amount) {
        if (amount <= 0) {
            return true;
        }
        long held = cashOnHand(player);
        if (held < amount) {
            return false;
        }
        // Hand over all notes, then give change: simple and exact. The player's note composition may
        // change, which is realistic enough for paying at a till.
        removeAllBanknotes(player);
        long change = held - amount;
        if (change > 0) {
            give(player, change);
        }
        return true;
    }

    /** Dispenses {@code amount} as banknotes, dropping any that do not fit in the player's inventory. */
    public void give(Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        Map<Integer, Integer> breakdown = Denominations.breakdown(amount, plugin.settings().denominations());
        for (Map.Entry<Integer, Integer> entry : breakdown.entrySet()) {
            int value = entry.getKey();
            int remaining = entry.getValue();
            int max = material.getMaxStackSize();
            while (remaining > 0) {
                int stack = Math.min(remaining, max);
                addOrDrop(player, create(value, stack));
                remaining -= stack;
            }
        }
    }

    private void removeAllBanknotes(Player player) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isBanknote(contents[slot])) {
                contents[slot] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private void addOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    /** Denominations available, descending. */
    public List<Integer> denominations() {
        return plugin.settings().denominations();
    }
}
