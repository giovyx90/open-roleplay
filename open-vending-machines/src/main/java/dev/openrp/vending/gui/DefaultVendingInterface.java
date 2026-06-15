package dev.openrp.vending.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.core.Authorization;
import dev.openrp.vending.core.MachineInfoPresenter;
import dev.openrp.vending.core.Money;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;

/**
 * Default chest-GUI implementation of {@link VendingInterface}. Two views share one click listener,
 * recognised via {@link VendingGuiHolder}. Every action delegates to the same locked, validated core
 * services the commands use, then rebuilds the view to reflect the new state. Replace the whole class
 * by setting a different {@link VendingInterface}; the core never references this type directly.
 */
public final class DefaultVendingInterface implements VendingInterface, Listener {

    private final OpenVendingMachinesPlugin plugin;

    public DefaultVendingInterface(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void openPurchase(Player player, VendingMachine machine) {
        player.openInventory(buildPurchaseView(player, machine));
    }

    @Override
    public void openManagement(Player player, VendingMachine machine) {
        player.openInventory(buildManageView(player, machine));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VendingGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        VendingMachine machine = plugin.machines().get(holder.machineId());
        if (machine == null) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        switch (holder.view()) {
            case PURCHASE -> handlePurchaseClick(player, machine, holder, slot);
            case MANAGE -> handleManageClick(player, machine, holder, slot,
                    event.isLeftClick(), event.isRightClick(), event.isShiftClick());
        }
    }

    private void handlePurchaseClick(Player player, VendingMachine machine, VendingGuiHolder holder, int slot) {
        if (slot == GuiLayout.MANAGE_BUTTON && canManage(player, machine)) {
            openManagement(player, machine);
            return;
        }
        String productId = holder.productAt(slot);
        if (productId != null) {
            plugin.purchases().purchase(player, machine, productId, 1);
            openPurchase(player, machine);
        }
    }

    private void handleManageClick(Player player, VendingMachine machine, VendingGuiHolder holder, int slot,
                                   boolean left, boolean right, boolean shift) {
        if (slot == GuiLayout.WITHDRAW_BUTTON) {
            plugin.cash().withdraw(player, machine);
            openManagement(player, machine);
            return;
        }
        if (slot == GuiLayout.BACK_BUTTON) {
            openPurchase(player, machine);
            return;
        }
        if (slot == GuiLayout.INFO_BUTTON) {
            MachineInfoPresenter.send(plugin, player, machine);
            return;
        }
        String productId = holder.productAt(slot);
        if (productId == null) {
            return;
        }
        if (left) {
            int amount = shift ? plugin.settings().restockMaxPerAction() : 1;
            plugin.restocks().restock(player, machine, productId, amount);
        } else if (right && plugin.settings().allowPriceEditing()) {
            MachineProduct product = machine.product(productId);
            if (product != null) {
                double delta = shift ? -1.0 : 1.0;
                plugin.restocks().setPrice(player, machine, productId, product.price() + delta);
            }
        }
        openManagement(player, machine);
    }

    private Inventory buildPurchaseView(Player player, VendingMachine machine) {
        VendingGuiHolder holder = new VendingGuiHolder(machine.id(), VendingGuiHolder.View.PURCHASE);
        Inventory inventory = Bukkit.createInventory(holder, GuiLayout.SIZE,
                plugin.messages().mini(player, "gui.purchase_title", "type", typeDisplay(machine)));
        holder.setInventory(inventory);
        fillProducts(player, machine, holder, inventory, false);
        if (canManage(player, machine)) {
            inventory.setItem(GuiLayout.MANAGE_BUTTON, button(Material.CHEST, plugin.messages().mini(player, "gui.manage_button")));
        }
        return inventory;
    }

    private Inventory buildManageView(Player player, VendingMachine machine) {
        VendingGuiHolder holder = new VendingGuiHolder(machine.id(), VendingGuiHolder.View.MANAGE);
        Inventory inventory = Bukkit.createInventory(holder, GuiLayout.SIZE,
                plugin.messages().mini(player, "gui.manage_title", "type", typeDisplay(machine)));
        holder.setInventory(inventory);
        fillProducts(player, machine, holder, inventory, true);

        ItemStack withdraw = button(Material.GOLD_INGOT, plugin.messages().mini(player, "gui.withdraw_button"));
        applyLore(withdraw, List.of(plugin.messages().mini(player, "gui.withdraw_lore",
                "symbol", plugin.settings().currencySymbol(), "cash", Money.format(machine.cashBalance()))));
        inventory.setItem(GuiLayout.WITHDRAW_BUTTON, withdraw);
        inventory.setItem(GuiLayout.INFO_BUTTON, button(Material.PAPER, plugin.messages().mini(player, "gui.info_button")));
        inventory.setItem(GuiLayout.BACK_BUTTON, button(Material.ARROW, plugin.messages().mini(player, "gui.back_button")));
        return inventory;
    }

    private void fillProducts(Player player, VendingMachine machine, VendingGuiHolder holder, Inventory inventory, boolean manage) {
        int slot = 0;
        for (MachineProduct product : machine.products()) {
            if (slot >= GuiLayout.PRODUCT_SLOTS) {
                break;
            }
            ProductDefinition definition = plugin.products().get(product.productId());
            if (definition == null) {
                continue;
            }
            inventory.setItem(slot, manage ? manageIcon(player, product, definition) : purchaseIcon(player, product, definition));
            holder.mapProduct(slot, product.productId());
            slot++;
        }
    }

    private ItemStack purchaseIcon(Player player, MachineProduct product, ProductDefinition definition) {
        ItemStack item = definition.createStack(1);
        List<Component> lore = baseLore(item);
        lore.add(plugin.messages().mini(player, "gui.buy_lore_price",
                "symbol", plugin.settings().currencySymbol(), "price", Money.format(product.price())));
        lore.add(plugin.messages().mini(player, "gui.buy_lore_stock",
                "stock", product.stock(), "capacity", product.capacity()));
        lore.add(product.inStock()
                ? plugin.messages().mini(player, "gui.buy_lore_click")
                : plugin.messages().mini(player, "gui.out_of_stock"));
        applyLore(item, lore);
        return item;
    }

    private ItemStack manageIcon(Player player, MachineProduct product, ProductDefinition definition) {
        ItemStack item = definition.createStack(1);
        List<Component> lore = baseLore(item);
        lore.add(plugin.messages().mini(player, "gui.buy_lore_price",
                "symbol", plugin.settings().currencySymbol(), "price", Money.format(product.price())));
        lore.add(plugin.messages().mini(player, "gui.buy_lore_stock",
                "stock", product.stock(), "capacity", product.capacity()));
        lore.add(plugin.messages().mini(player, "gui.manage_lore_restock", "max", plugin.settings().restockMaxPerAction()));
        if (plugin.settings().allowPriceEditing()) {
            lore.add(plugin.messages().mini(player, "gui.manage_lore_price"));
        }
        applyLore(item, lore);
        return item;
    }

    private boolean canManage(Player player, VendingMachine machine) {
        return Authorization.canRestock(plugin, player, machine)
                || Authorization.canWithdraw(plugin, player, machine)
                || Authorization.canEditPrice(plugin, player, machine);
    }

    private String typeDisplay(VendingMachine machine) {
        MachineType type = plugin.machineTypes().get(machine.typeId());
        return type == null ? machine.typeId() : type.displayName();
    }

    private static List<Component> baseLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.lore() != null) {
            return new ArrayList<>(meta.lore());
        }
        return new ArrayList<>();
    }

    private static ItemStack button(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyLore(ItemStack item, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }
}
