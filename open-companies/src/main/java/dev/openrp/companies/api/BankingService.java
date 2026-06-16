package dev.openrp.companies.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import dev.openrp.companies.core.CompanyResult;

/**
 * Personal banking, the player-facing counterpart of {@link TreasuryService}. A player has two money
 * pools: physical cash (banknote items in their inventory) and a personal bank account (held in the
 * economy adapter, drawn on by their payment card). This service bridges the two at an ATM/bank
 * interaction - turning cash into bank balance and back - and issues payment cards. As with everything
 * in this module it is diegetic: these methods are invoked from an in-world ATM screen, not a command.
 */
public interface BankingService {

    /** The player's personal bank-account balance. */
    double bankBalance(OfflinePlayer player);

    /** Total face value of banknotes the player is carrying. */
    long cashOnHand(Player player);

    /** Pays cash banknotes into the player's bank account. */
    CompanyResult deposit(Player player, long amount);

    /** Withdraws bank balance as dispensed banknotes (plus any configured ATM fee). */
    CompanyResult withdraw(Player player, long amount);

    /** Issues a payment card bound to the player. */
    CompanyResult issueCard(Player player);
}
