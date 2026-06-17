package dev.openrp.crime.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import dev.openrp.crime.adapter.defaults.InternalEconomyAdapter;
import dev.openrp.crime.adapter.defaults.MemoryStorageAdapter;

public class InternalEconomyAdapterTest {

    @Test
    public void depositWithdrawKeepsDirtyAndCleanSeparate() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        InternalEconomyAdapter economy = new InternalEconomyAdapter(storage);
        economy.load();
        UUID treasury = UUID.randomUUID();

        economy.deposit(treasury, 1000L, true);
        economy.deposit(treasury, 200L, false);
        assertEquals(1000L, economy.balance(treasury, true));
        assertEquals(200L, economy.balance(treasury, false));

        assertFalse("cannot overdraw", economy.withdraw(treasury, 2000L, true));
        assertTrue(economy.withdraw(treasury, 600L, true));
        assertEquals(400L, economy.balance(treasury, true));
    }

    @Test
    public void depositSaturatesInsteadOfOverflowing() {
        InternalEconomyAdapter economy = new InternalEconomyAdapter(new MemoryStorageAdapter());
        economy.load();
        UUID treasury = UUID.randomUUID();
        economy.deposit(treasury, Long.MAX_VALUE, true);
        economy.deposit(treasury, 1_000L, true);
        assertEquals(Long.MAX_VALUE, economy.balance(treasury, true));
    }

    @Test
    public void balancesSurviveReloadFromStorage() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        InternalEconomyAdapter economy = new InternalEconomyAdapter(storage);
        economy.load();
        UUID treasury = UUID.randomUUID();
        economy.deposit(treasury, 500L, true);

        InternalEconomyAdapter reloaded = new InternalEconomyAdapter(storage);
        reloaded.load();
        assertEquals(500L, reloaded.balance(treasury, true));
    }
}
