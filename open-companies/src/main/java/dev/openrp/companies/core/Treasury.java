package dev.openrp.companies.core;

import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.config.CompaniesSettings;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.TransactionType;

/**
 * Pure rule engine for a company's money, mirroring how {@link CompanyManager} is the pure engine for
 * membership. It validates amounts, moves the treasury balance on the {@link Company} aggregate,
 * persists the company and appends a {@link CompanyTransaction} to the {@link LedgerManager}. It does
 * <em>not</em> lock or fire Bukkit events - {@link DefaultTreasuryService} wraps each call with the
 * company lock and fires {@code CompanyTransactionEvent} for the returned line. Keeping the arithmetic
 * here (no plugin, no events) makes it fully unit-testable.
 *
 * <p>The two player-bridging methods are atomic across the company/player boundary: the single fallible
 * player-side economy step is ordered so that a failure leaves both sides untouched.</p>
 */
public final class Treasury {

    private final CompanyManager companies;
    private final LedgerManager ledger;
    private final AdapterRegistry adapters;
    private final CompaniesSettings settings;

    public Treasury(CompanyManager companies, LedgerManager ledger, AdapterRegistry adapters,
                    CompaniesSettings settings) {
        this.companies = companies;
        this.ledger = ledger;
        this.adapters = adapters;
        this.settings = settings;
    }

    public double balance(String companyId) {
        return companies.company(companyId).map(Company::balance).orElse(0.0);
    }

    public CompanyResult deposit(String companyId, double amount, TransactionType type, UUID actor, String note) {
        return mutate(companyId, amount, type, actor, null, note, false);
    }

    public CompanyResult withdraw(String companyId, double amount, TransactionType type, UUID actor, String note) {
        return mutate(companyId, amount, type, actor, null, note, true);
    }

    public CompanyResult collectFromPlayer(String companyId, OfflinePlayer payer, double amount,
                                           TransactionType type, UUID actor, String note) {
        if (payer == null) {
            return CompanyResult.fail("treasury.invalid_target");
        }
        if (!validAmount(amount)) {
            return CompanyResult.fail("treasury.invalid_amount");
        }
        Company company = companies.company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        // Take from the payer first: it is the only fallible step. Crediting the treasury below cannot
        // fail, so a successful withdrawal is never left without a matching credit.
        if (!adapters.economy().withdraw(payer, settings.bankAccount(), amount)) {
            return CompanyResult.fail("treasury.payer_insufficient");
        }
        company.setBalance(company.balance() + amount);
        companies.persist(company);
        return record(company, amount, type, actor, payer.getUniqueId().toString(), note, "treasury.collected");
    }

    public CompanyResult transferToPlayer(String companyId, OfflinePlayer target, double amount,
                                          TransactionType type, UUID actor, String note) {
        if (target == null) {
            return CompanyResult.fail("treasury.invalid_target");
        }
        if (!validAmount(amount)) {
            return CompanyResult.fail("treasury.invalid_amount");
        }
        Company company = companies.company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (company.balance() < amount) {
            return CompanyResult.fail("treasury.insufficient_funds", "amount", format(amount),
                    "balance", format(company.balance()));
        }
        // Debit the treasury, then pay the player. If the payee deposit fails, restore the treasury so
        // the net effect is nothing - no money vanishes.
        company.setBalance(company.balance() - amount);
        companies.persist(company);
        if (!adapters.economy().deposit(target, settings.bankAccount(), amount)) {
            company.setBalance(company.balance() + amount);
            companies.persist(company);
            return CompanyResult.fail("treasury.transfer_failed");
        }
        return record(company, amount, type, actor, target.getUniqueId().toString(), note, "treasury.transferred");
    }

    public List<CompanyTransaction> transactions(String companyId, int limit) {
        return ledger.recent(companyId, limit);
    }

    // --- internals ---------------------------------------------------------------------------

    private CompanyResult mutate(String companyId, double amount, TransactionType type, UUID actor,
                                 String counterparty, String note, boolean debit) {
        if (!validAmount(amount)) {
            return CompanyResult.fail("treasury.invalid_amount");
        }
        Company company = companies.company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (debit && company.balance() < amount) {
            return CompanyResult.fail("treasury.insufficient_funds", "amount", format(amount),
                    "balance", format(company.balance()));
        }
        company.setBalance(company.balance() + (debit ? -amount : amount));
        companies.persist(company);
        return record(company, amount, type, actor, counterparty, note,
                debit ? "treasury.withdrawn" : "treasury.deposited");
    }

    private CompanyResult record(Company company, double amount, TransactionType type, UUID actor,
                                 String counterparty, String note, String okKey) {
        CompanyTransaction transaction = new CompanyTransaction(UUID.randomUUID(), company.id(),
                System.currentTimeMillis(), type, amount, actor, counterparty, note);
        ledger.append(transaction);
        return CompanyResult.ok(okKey, "company", company.displayName(), "amount", format(amount))
                .withPayload(transaction);
    }

    private static boolean validAmount(double amount) {
        return Double.isFinite(amount) && amount > 0.0;
    }

    private String format(double value) {
        return settings.currencySymbol() + String.format("%.2f", value);
    }
}
