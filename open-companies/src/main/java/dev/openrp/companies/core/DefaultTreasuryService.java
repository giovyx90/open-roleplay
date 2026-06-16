package dev.openrp.companies.core;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.api.TreasuryService;
import dev.openrp.companies.event.CompanyTransactionEvent;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.TransactionType;

/**
 * Bukkit-facing wrapper around the pure {@link Treasury} engine. It adds the framework concerns the
 * engine deliberately omits: each mutation runs under the company lock (so concurrent interactions on
 * the same company can never corrupt its balance), and a successful movement fires a
 * {@link CompanyTransactionEvent} so other modules can react. The arithmetic itself lives in
 * {@link Treasury} and is unit-tested without a server.
 */
public final class DefaultTreasuryService implements TreasuryService {

    private final OpenCompaniesPlugin plugin;
    private final Treasury treasury;

    public DefaultTreasuryService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
        this.treasury = new Treasury(plugin.companyManager(), plugin.ledger(), plugin.adapters(), plugin.settings());
    }

    @Override
    public double balance(String companyId) {
        return treasury.balance(companyId);
    }

    @Override
    public CompanyResult deposit(String companyId, double amount, TransactionType type, UUID actor, String note) {
        return locked(companyId, () -> treasury.deposit(companyId, amount, type, actor, note));
    }

    @Override
    public CompanyResult withdraw(String companyId, double amount, TransactionType type, UUID actor, String note) {
        return locked(companyId, () -> treasury.withdraw(companyId, amount, type, actor, note));
    }

    @Override
    public CompanyResult collectFromPlayer(String companyId, OfflinePlayer payer, double amount,
                                           TransactionType type, UUID actor, String note) {
        return locked(companyId, () -> treasury.collectFromPlayer(companyId, payer, amount, type, actor, note));
    }

    @Override
    public CompanyResult transferToPlayer(String companyId, OfflinePlayer target, double amount,
                                          TransactionType type, UUID actor, String note) {
        return locked(companyId, () -> treasury.transferToPlayer(companyId, target, amount, type, actor, note));
    }

    @Override
    public List<CompanyTransaction> transactions(String companyId, int limit) {
        return treasury.transactions(companyId, limit);
    }

    /** Runs a treasury mutation under the company lock and fires the event for the resulting line. */
    private CompanyResult locked(String companyId, Supplier<CompanyResult> action) {
        ReentrantLock lock = plugin.locks().get(companyId);
        lock.lock();
        try {
            CompanyResult result = action.get();
            result.transaction().ifPresent(transaction -> {
                Company company = plugin.companyManager().company(companyId).orElse(null);
                if (company != null) {
                    plugin.getServer().getPluginManager().callEvent(new CompanyTransactionEvent(company, transaction));
                }
            });
            return result;
        } finally {
            lock.unlock();
        }
    }
}
