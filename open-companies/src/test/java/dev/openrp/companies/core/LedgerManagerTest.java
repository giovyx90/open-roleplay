package dev.openrp.companies.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.TransactionType;

/**
 * Verifies the in-memory ledger cache: append order, newest-first paging with a limit, per-company
 * isolation, write-through to storage (a fresh manager reloads what was appended) and bulk removal.
 */
public class LedgerManagerTest {

    private AdapterRegistry adapters;
    private LedgerManager ledger;

    @Before
    public void setUp() {
        adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        ledger = new LedgerManager(adapters);
    }

    private CompanyTransaction line(String company, long timestamp, TransactionType type) {
        return new CompanyTransaction(UUID.randomUUID(), company, timestamp, type, 10.0, null, null, "");
    }

    @Test
    public void recentReturnsNewestFirstAndRespectsLimit() {
        ledger.append(line("acme", 1L, TransactionType.SALE_CASH));
        ledger.append(line("acme", 2L, TransactionType.SALARY));
        ledger.append(line("acme", 3L, TransactionType.FEE));

        List<CompanyTransaction> recent = ledger.recent("acme", 2);
        assertEquals(2, recent.size());
        assertEquals(3L, recent.get(0).timestamp());
        assertEquals(2L, recent.get(1).timestamp());
        assertEquals(3, ledger.count("acme"));
    }

    @Test
    public void ledgerIsPerCompany() {
        ledger.append(line("acme", 1L, TransactionType.SALE_CASH));
        ledger.append(line("globex", 1L, TransactionType.SALE_CARD));
        assertEquals(1, ledger.recent("acme", 10).size());
        assertEquals(1, ledger.recent("globex", 10).size());
        assertTrue(ledger.recent("nobody", 10).isEmpty());
    }

    @Test
    public void appendWritesThroughToStorageSoReloadSeesIt() {
        ledger.append(line("acme", 1L, TransactionType.SALE_CASH));

        LedgerManager reloaded = new LedgerManager(adapters);
        reloaded.loadAll();
        assertEquals(1, reloaded.recent("acme", 10).size());
    }

    @Test
    public void removeAllOfClearsCacheAndStorage() {
        ledger.append(line("acme", 1L, TransactionType.SALE_CASH));
        ledger.append(line("acme", 2L, TransactionType.SALARY));
        ledger.removeAllOf("acme");

        assertEquals(0, ledger.count("acme"));
        LedgerManager reloaded = new LedgerManager(adapters);
        reloaded.loadAll();
        assertTrue(reloaded.recent("acme", 10).isEmpty());
    }
}
