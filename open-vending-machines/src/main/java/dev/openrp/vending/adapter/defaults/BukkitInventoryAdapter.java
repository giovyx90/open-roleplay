package dev.openrp.vending.adapter.defaults;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.openrp.vending.adapter.InventoryAdapter;
import dev.openrp.vending.model.ProductDefinition;

/**
 * Default inventory adapter backed by real Bukkit player inventories.
 *
 * <p>Quantities are expressed in <b>units</b>; one unit is the {@link ProductDefinition}'s configured
 * item stack ({@code amount} items of {@code material}). Items are matched with
 * {@link ItemStack#isSimilar(ItemStack)} against the product's canonical stack, so restocking
 * consumes exactly the items the machine dispenses. Capacity is checked before any item moves, so
 * {@link #give} and {@link #take} are all-or-nothing.</p>
 */
public final class BukkitInventoryAdapter implements InventoryAdapter {

    @Override
    public String id() {
        return "bukkit";
    }

    @Override
    public boolean canReceive(Player player, ProductDefinition product, int amount) {
        if (amount <= 0) {
            return true;
        }
        ItemStack template = product.createStack(product.amount());
        return freeItemSpace(player, template) >= amount * product.amount();
    }

    @Override
    public boolean give(Player player, ProductDefinition product, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!canReceive(player, product, amount)) {
            return false;
        }
        ItemStack[] stacks = new ItemStack[amount];
        for (int i = 0; i < amount; i++) {
            stacks[i] = product.createStack(product.amount());
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks);
        // The pre-check guarantees space on the single server thread, so leftover is empty.
        return leftover.isEmpty();
    }

    @Override
    public boolean has(Player player, ProductDefinition product, int amount) {
        if (amount <= 0) {
            return true;
        }
        ItemStack template = product.createStack(product.amount());
        return countItems(player, template) >= amount * product.amount();
    }

    @Override
    public boolean take(Player player, ProductDefinition product, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!has(player, product, amount)) {
            return false;
        }
        ItemStack template = product.createStack(product.amount());
        int toRemove = amount * product.amount();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && template.isSimilar(stack)) {
                int removed = Math.min(toRemove, stack.getAmount());
                stack.setAmount(stack.getAmount() - removed);
                toRemove -= removed;
                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setStorageContents(contents);
        return toRemove == 0;
    }

    private int freeItemSpace(Player player, ItemStack template) {
        int max = template.getMaxStackSize();
        int free = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                free += max;
            } else if (template.isSimilar(stack)) {
                free += Math.max(0, max - stack.getAmount());
            }
        }
        return free;
    }

    private int countItems(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && template.isSimilar(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
