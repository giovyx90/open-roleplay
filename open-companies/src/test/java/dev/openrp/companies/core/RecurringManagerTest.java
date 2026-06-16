package dev.openrp.companies.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.companies.model.RecurringPayment;

/**
 * Verifies recurring-salary storage and the due/skip arithmetic the payroll task relies on: due
 * detection, scheduling the next run, write-through persistence and bulk removal on company deletion.
 */
public class RecurringManagerTest {

    private AdapterRegistry adapters;
    private RecurringManager recurring;

    @Before
    public void setUp() {
        adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        recurring = new RecurringManager(adapters);
    }

    @Test
    public void dueDetectionRespectsAmountAndTime() {
        UUID member = UUID.randomUUID();
        RecurringPayment payment = new RecurringPayment("acme", member, 100.0, 60, 1_000L);
        assertTrue(payment.isDue(1_000L));
        assertTrue(payment.isDue(5_000L));
        assertFalse(payment.isDue(999L));

        payment.scheduleNext(5_000L);
        assertEquals(5_000L + 60_000L, payment.nextDueAt());
        assertFalse(payment.isDue(5_000L));

        // A zero amount is never due even when its time has passed.
        assertFalse(new RecurringPayment("acme", member, 0.0, 60, 0L).isDue(10_000L));
    }

    @Test
    public void setRemoveAndReloadRoundTrip() {
        UUID member = UUID.randomUUID();
        recurring.set(new RecurringPayment("acme", member, 250.0, 3600, 0L));
        assertTrue(recurring.get("acme", member).isPresent());
        assertEquals(1, recurring.due(10_000L).size());

        RecurringManager reloaded = new RecurringManager(adapters);
        reloaded.loadAll();
        assertEquals(250.0, reloaded.get("acme", member).orElseThrow().amount(), 1e-9);

        reloaded.remove("acme", member);
        assertTrue(reloaded.get("acme", member).isEmpty());
        RecurringManager afterDelete = new RecurringManager(adapters);
        afterDelete.loadAll();
        assertTrue(afterDelete.get("acme", member).isEmpty());
    }

    @Test
    public void removeAllOfClearsOnlyThatCompany() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        recurring.set(new RecurringPayment("acme", a, 10.0, 60, 0L));
        recurring.set(new RecurringPayment("globex", b, 10.0, 60, 0L));
        recurring.removeAllOf("acme");
        assertTrue(recurring.get("acme", a).isEmpty());
        assertTrue(recurring.get("globex", b).isPresent());
    }
}
