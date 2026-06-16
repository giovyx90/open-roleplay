package dev.openrp.companies.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.TransactionType;

/**
 * The customer-facing pay screen of a POS once a cashier has rung up a charge. The customer settles it
 * by card (drawing on their own bank account, requiring they hold their payment card) or by cash
 * (consuming banknotes). Either way the money lands in the company treasury and the pending charge is
 * cleared. Pure in-world: the only inputs are the two pay buttons.
 */
public final class PosPayMenu extends Menu {

    private static final int SIZE = 27;
    private static final int PAY_CARD = 11;
    private static final int DISPLAY = 13;
    private static final int PAY_CASH = 15;
    private static final int CANCEL = 22;

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final CompanyAsset asset;
    private final long amount;
    private final CompanyMenus menus;

    public PosPayMenu(OpenCompaniesPlugin plugin, Player player, CompanyAsset asset, long amount, CompanyMenus menus) {
        this.plugin = plugin;
        this.player = player;
        this.asset = asset;
        this.amount = amount;
        this.menus = menus;
        this.inventory = Bukkit.createInventory(this, SIZE,
                plugin.messages().mini(player, "pos.pay_title"));
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, Items.filler());
        }
        String price = plugin.settings().currencySymbol() + amount;
        inventory.setItem(DISPLAY, Items.button(Material.PAPER,
                Component.text(price, NamedTextColor.GREEN)));
        inventory.setItem(PAY_CARD, Items.button(Material.PAPER,
                Component.text("Paga con carta", NamedTextColor.AQUA)));
        inventory.setItem(PAY_CASH, Items.button(Material.GOLD_INGOT,
                Component.text("Paga in contanti", NamedTextColor.GOLD)));
        inventory.setItem(CANCEL, Items.button(Material.BARRIER,
                Component.text("Annulla", NamedTextColor.RED)));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == CANCEL) {
            player.closeInventory();
            return;
        }
        if (slot == PAY_CARD) {
            payByCard();
        } else if (slot == PAY_CASH) {
            payByCash();
        }
    }

    private void payByCard() {
        if (!holdsOwnCard()) {
            plugin.messages().error(player, "pos.no_card");
            return;
        }
        CompanyResult result = plugin.treasury().collectFromPlayer(asset.companyId(), player, amount,
                TransactionType.SALE_CARD, player.getUniqueId(), "POS");
        settle(result);
    }

    private void payByCash() {
        if (plugin.banknotes().cashOnHand(player) < amount) {
            plugin.messages().error(player, "pos.no_cash");
            return;
        }
        if (!plugin.banknotes().take(player, amount)) {
            plugin.messages().error(player, "pos.no_cash");
            return;
        }
        CompanyResult result = plugin.treasury().deposit(asset.companyId(), amount,
                TransactionType.SALE_CASH, player.getUniqueId(), "POS");
        if (result.failed()) {
            // Crediting the treasury failed after taking cash: hand the money back so nothing is lost.
            plugin.banknotes().give(player, amount);
        }
        settle(result);
    }

    private void settle(CompanyResult result) {
        if (result.success()) {
            menus.clearCharge(asset.id());
            plugin.messages().success(player, "pos.paid",
                    "amount", plugin.settings().currencySymbol() + amount);
            player.closeInventory();
        } else {
            plugin.messages().error(player, result.messageKey(), result.placeholders());
        }
    }

    private boolean holdsOwnCard() {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (plugin.cards().ownerOf(item).filter(player.getUniqueId()::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
