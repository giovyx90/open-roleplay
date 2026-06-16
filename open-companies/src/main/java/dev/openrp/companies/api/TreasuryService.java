package dev.openrp.companies.api;

import java.util.List;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.TransactionType;

/**
 * The single, locked entry point for a company's money. The treasury balance lives on the company
 * aggregate ({@code Company#balance()}); this service is the only thing that mutates it, always under
 * the company lock, always appending a {@link CompanyTransaction} ledger line and firing a
 * {@code CompanyTransactionEvent}. Player-side money (personal bank accounts) is moved through the
 * economy adapter, so {@link #collectFromPlayer} and {@link #transferToPlayer} bridge the two sides
 * atomically - if either half fails the whole movement is rolled back and no money is created or lost.
 *
 * <p>Diegetic by design: there is no money command. POS/registers, the company terminal and ATMs call
 * these methods after an in-world interaction; the amount is whatever the player typed on the device's
 * on-screen keypad.</p>
 */
public interface TreasuryService {

    /** Current treasury balance of the company, or {@code 0} if it does not exist. */
    double balance(String companyId);

    /**
     * Adds money to the treasury (e.g. a cash sale whose banknotes were already consumed, or a director
     * feeding physical cash in). The matching real-world source is the caller's responsibility.
     */
    CompanyResult deposit(String companyId, double amount, TransactionType type, java.util.UUID actor, String note);

    /**
     * Removes money from the treasury (e.g. dispensing physical cash to a director). The matching
     * real-world destination is the caller's responsibility. Fails on insufficient funds.
     */
    CompanyResult withdraw(String companyId, double amount, TransactionType type, java.util.UUID actor, String note);

    /**
     * Atomically takes {@code amount} from {@code payer}'s bank account and credits the treasury. Used
     * for card sales at a POS/register. Refunds the payer if the treasury credit cannot be applied.
     */
    CompanyResult collectFromPlayer(String companyId, OfflinePlayer payer, double amount,
                                    TransactionType type, java.util.UUID actor, String note);

    /**
     * Atomically takes {@code amount} from the treasury and credits {@code target}'s bank account. Used
     * for discretionary salaries/bonifici from the company terminal. Refunds the treasury if the payee
     * deposit cannot be applied. Fails on insufficient treasury funds.
     */
    CompanyResult transferToPlayer(String companyId, OfflinePlayer target, double amount,
                                   TransactionType type, java.util.UUID actor, String note);

    /** The most recent ledger lines for a company, newest first, capped at {@code limit}. */
    List<CompanyTransaction> transactions(String companyId, int limit);
}
