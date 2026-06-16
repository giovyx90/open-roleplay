package dev.openrp.companies.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.model.CompanyTransaction;

/**
 * In-memory cache of the treasury ledger, mirroring how {@link CompanyManager} and {@link AssetManager}
 * keep their aggregates in RAM and write through to the storage adapter. Transactions are grouped by
 * company in chronological (append) order; reads return the most recent lines newest-first without
 * touching disk. The ledger is append-only - a line is never updated, only added or (on company
 * deletion) bulk-removed.
 *
 * <p>Not thread-safe on its own; callers append while holding the relevant company lock (see
 * {@link CompanyLocks}), exactly like the other managers.</p>
 */
public final class LedgerManager {

    private final AdapterRegistry adapters;
    private final Map<String, List<CompanyTransaction>> byCompany = new LinkedHashMap<>();

    public LedgerManager(AdapterRegistry adapters) {
        this.adapters = adapters;
    }

    /** (Re)loads every ledger line from storage into memory. Called on enable and on reload. */
    public void loadAll() {
        byCompany.clear();
        for (CompanyTransaction transaction : adapters.storage().loadTransactions()) {
            byCompany.computeIfAbsent(transaction.companyId(), key -> new ArrayList<>()).add(transaction);
        }
    }

    /** Appends a line to the cache and persists it. */
    public void append(CompanyTransaction transaction) {
        byCompany.computeIfAbsent(transaction.companyId(), key -> new ArrayList<>()).add(transaction);
        adapters.storage().appendTransaction(transaction);
    }

    /** The {@code limit} most recent lines for a company, newest first. */
    public List<CompanyTransaction> recent(String companyId, int limit) {
        List<CompanyTransaction> lines = byCompany.get(companyId);
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        int from = limit <= 0 ? 0 : Math.max(0, lines.size() - limit);
        List<CompanyTransaction> page = new ArrayList<>(lines.subList(from, lines.size()));
        Collections.reverse(page);
        return page;
    }

    /** Total number of ledger lines a company has. */
    public int count(String companyId) {
        List<CompanyTransaction> lines = byCompany.get(companyId);
        return lines == null ? 0 : lines.size();
    }

    /** Drops every line of a company (on deletion) from cache and storage. */
    public void removeAllOf(String companyId) {
        byCompany.remove(companyId);
        adapters.storage().deleteTransactionsOf(companyId);
    }
}
