package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyTransaction;

/**
 * Fired after a treasury movement has been applied and persisted - it reports a fact, so it is not
 * cancellable. Listeners can mirror the movement into their own systems (dynamic prefixes, dashboards,
 * tax modules) without re-running the financial logic. The {@link CompanyTransaction} carries the full
 * detail; {@link #getCompany()} carries the up-to-date company (its {@link Company#balance()} already
 * reflects this movement).
 */
public final class CompanyTransactionEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyTransaction transaction;

    public CompanyTransactionEvent(Company company, CompanyTransaction transaction) {
        super(company);
        this.transaction = transaction;
    }

    public CompanyTransaction getTransaction() {
        return transaction;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
