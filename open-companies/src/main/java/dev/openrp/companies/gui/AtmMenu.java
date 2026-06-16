package dev.openrp.companies.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.core.CompanyResult;

/**
 * The ATM screen: personal banking, open to anyone. A player can pay cash into their bank account,
 * withdraw bank balance as banknotes, check their balances and have a payment card issued. Amounts are
 * entered on the shared {@link KeypadMenu}. Backed by {@link dev.openrp.companies.api.BankingService}.
 */
public final class AtmMenu extends Menu {

    private static final int SIZE = 27;
    private static final int BALANCE = 4;
    private static final int DEPOSIT = 10;
    private static final int WITHDRAW = 12;
    private static final int CARD = 14;
    private static final int CLOSE = 22;

    private final OpenCompaniesPlugin plugin;
    private final Player player;

    public AtmMenu(OpenCompaniesPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, plugin.messages().mini(player, "atm.title"));
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, Items.filler());
        }
        String symbol = plugin.settings().currencySymbol();
        String bank = symbol + String.format("%.2f", plugin.banking().bankBalance(player));
        String cash = symbol + plugin.banking().cashOnHand(player);
        inventory.setItem(BALANCE, Items.button(Material.GOLD_BLOCK,
                Component.text("Conto: " + bank, NamedTextColor.GREEN),
                Component.text("Contante: " + cash, NamedTextColor.YELLOW)));
        inventory.setItem(DEPOSIT, Items.button(Material.HOPPER,
                Component.text("Versa contante", NamedTextColor.GREEN)));
        inventory.setItem(WITHDRAW, Items.button(Material.DISPENSER,
                Component.text("Preleva", NamedTextColor.RED)));
        inventory.setItem(CARD, Items.button(Material.PAPER,
                Component.text("Richiedi carta", NamedTextColor.AQUA)));
        inventory.setItem(CLOSE, Items.button(Material.BARRIER,
                Component.text("Chiudi", NamedTextColor.GRAY)));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        long max = (long) plugin.settings().keypadMaxAmount();
        switch (event.getRawSlot()) {
            case DEPOSIT -> new KeypadMenu(plugin, player, "Versa contante", max,
                    amount -> send(plugin.banking().deposit(player, amount)), this::reopen).open(player);
            case WITHDRAW -> new KeypadMenu(plugin, player, "Preleva", max,
                    amount -> send(plugin.banking().withdraw(player, amount)), this::reopen).open(player);
            case CARD -> {
                send(plugin.banking().issueCard(player));
                reopen();
            }
            case CLOSE -> player.closeInventory();
            default -> {
                // decorative slot
            }
        }
    }

    private void reopen() {
        new AtmMenu(plugin, player).open(player);
    }

    private void send(CompanyResult result) {
        if (result.success()) {
            plugin.messages().success(player, result.messageKey(), result.placeholders());
        } else {
            plugin.messages().error(player, result.messageKey(), result.placeholders());
        }
    }
}
