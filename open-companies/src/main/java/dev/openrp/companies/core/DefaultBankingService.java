package dev.openrp.companies.core;

import java.util.Map;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.api.BankingService;

/**
 * Default {@link BankingService}: bridges a player's physical cash ({@link dev.openrp.companies.item.Banknotes})
 * and their personal bank account (the economy adapter's {@code finance.bank-account}). Each operation
 * is ordered so the single fallible step leaves no money created or destroyed - cash taken is refunded
 * if the bank credit fails, and bank balance is only debited once the dispensing can proceed.
 */
public final class DefaultBankingService implements BankingService {

    private final OpenCompaniesPlugin plugin;

    public DefaultBankingService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    private String account() {
        return plugin.settings().bankAccount();
    }

    @Override
    public double bankBalance(OfflinePlayer player) {
        return plugin.adapters().economy().balance(player, account());
    }

    @Override
    public long cashOnHand(Player player) {
        return plugin.banknotes().cashOnHand(player);
    }

    @Override
    public CompanyResult deposit(Player player, long amount) {
        if (amount <= 0) {
            return CompanyResult.fail("bank.invalid_amount");
        }
        if (!plugin.banknotes().take(player, amount)) {
            return CompanyResult.fail("bank.not_enough_cash");
        }
        if (!plugin.adapters().economy().deposit(player, account(), amount)) {
            // Bank credit refused: hand the cash back so nothing is lost.
            plugin.banknotes().give(player, amount);
            return CompanyResult.fail("bank.deposit_failed");
        }
        return CompanyResult.ok("bank.deposited", "amount", format(amount));
    }

    @Override
    public CompanyResult withdraw(Player player, long amount) {
        if (amount <= 0) {
            return CompanyResult.fail("bank.invalid_amount");
        }
        double fee = plugin.settings().atmWithdrawFee();
        if (!plugin.adapters().economy().withdraw(player, account(), amount + fee)) {
            return CompanyResult.fail("bank.insufficient", "amount", format(amount + (long) Math.ceil(fee)));
        }
        plugin.banknotes().give(player, amount);
        return CompanyResult.ok("bank.withdrawn", "amount", format(amount));
    }

    @Override
    public CompanyResult issueCard(Player player) {
        ItemStack card = plugin.cards().create(player, player.getName());
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(card);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        return CompanyResult.ok("bank.card_issued");
    }

    private String format(long value) {
        return plugin.settings().currencySymbol() + value;
    }
}
