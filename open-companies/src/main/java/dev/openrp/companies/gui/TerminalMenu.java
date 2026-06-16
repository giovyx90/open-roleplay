package dev.openrp.companies.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyCapability;
import dev.openrp.companies.model.TransactionType;

/**
 * The company terminal: a director's on-screen control panel for the treasury. From here they pay
 * discretionary salaries/bonifici (pick a member, type the amount), review the ledger, and move
 * physical cash in and out of the treasury. Every amount is entered on the shared {@link KeypadMenu};
 * nothing here is a command. Opening requires {@link CompanyCapability#MANAGE_FINANCE}.
 */
public final class TerminalMenu extends Menu {

    private static final int SIZE = 27;
    private static final int BALANCE = 4;
    private static final int TRANSFER = 10;
    private static final int LEDGER = 12;
    private static final int DEPOSIT_CASH = 14;
    private static final int WITHDRAW_CASH = 16;
    private static final int RECURRING = 22;

    private final OpenCompaniesPlugin plugin;
    private final Player player;
    private final String companyId;

    public TerminalMenu(OpenCompaniesPlugin plugin, Player player, String companyId) {
        this.plugin = plugin;
        this.player = player;
        this.companyId = companyId;
        this.inventory = Bukkit.createInventory(this, SIZE,
                plugin.messages().mini(player, "terminal.title"));
        render();
    }

    private void render() {
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, Items.filler());
        }
        Company company = plugin.companies().findById(companyId).orElse(null);
        String name = company == null ? companyId : company.displayName();
        String balance = plugin.settings().currencySymbol() + String.format("%.2f", plugin.treasury().balance(companyId));
        inventory.setItem(BALANCE, Items.button(Material.GOLD_BLOCK,
                Component.text(name, NamedTextColor.GOLD),
                Component.text("Cassa: " + balance, NamedTextColor.GREEN)));
        inventory.setItem(TRANSFER, Items.button(Material.PAPER,
                Component.text("Bonifico / stipendio", NamedTextColor.AQUA)));
        inventory.setItem(LEDGER, Items.button(Material.WRITABLE_BOOK,
                Component.text("Movimenti", NamedTextColor.YELLOW)));
        inventory.setItem(DEPOSIT_CASH, Items.button(Material.HOPPER,
                Component.text("Versa contante", NamedTextColor.GREEN)));
        inventory.setItem(WITHDRAW_CASH, Items.button(Material.DISPENSER,
                Component.text("Preleva contante", NamedTextColor.RED)));
        inventory.setItem(RECURRING, Items.button(Material.CLOCK,
                Component.text("Stipendi ricorrenti", NamedTextColor.LIGHT_PURPLE)));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        switch (slot) {
            case TRANSFER -> new MemberPickerMenu(plugin, player, companyId).open(player);
            case LEDGER -> new LedgerMenu(plugin, player, companyId, 0).open(player);
            case DEPOSIT_CASH -> depositCash();
            case WITHDRAW_CASH -> withdrawCash();
            case RECURRING -> new RecurringMenu(plugin, player, companyId).open(player);
            default -> {
                // decorative slot
            }
        }
    }

    private void depositCash() {
        long max = (long) plugin.settings().keypadMaxAmount();
        new KeypadMenu(plugin, player, "Versa contante", max, amount -> {
            if (plugin.banknotes().cashOnHand(player) < amount || !plugin.banknotes().take(player, amount)) {
                plugin.messages().error(player, "bank.not_enough_cash");
                return;
            }
            CompanyResult result = plugin.treasury().deposit(companyId, amount,
                    TransactionType.TREASURY_DEPOSIT, player.getUniqueId(), "cash in");
            if (result.failed()) {
                plugin.banknotes().give(player, amount);
            }
            send(result);
        }, this::reopen).open(player);
    }

    private void withdrawCash() {
        long max = (long) plugin.settings().keypadMaxAmount();
        new KeypadMenu(plugin, player, "Preleva contante", max, amount -> {
            CompanyResult result = plugin.treasury().withdraw(companyId, amount,
                    TransactionType.TREASURY_WITHDRAW, player.getUniqueId(), "cash out");
            if (result.success()) {
                plugin.banknotes().give(player, amount);
            }
            send(result);
        }, this::reopen).open(player);
    }

    private void reopen() {
        new TerminalMenu(plugin, player, companyId).open(player);
    }

    private void send(CompanyResult result) {
        if (result.success()) {
            plugin.messages().success(player, result.messageKey(), result.placeholders());
        } else {
            plugin.messages().error(player, result.messageKey(), result.placeholders());
        }
    }
}
