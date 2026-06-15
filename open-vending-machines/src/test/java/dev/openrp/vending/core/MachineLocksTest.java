package dev.openrp.vending.core;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Test;

public class MachineLocksTest {

    @Test
    public void sameIdReturnsSameLock() {
        MachineLocks locks = new MachineLocks();
        UUID id = UUID.randomUUID();
        assertSame(locks.get(id), locks.get(id));
    }

    @Test
    public void differentIdsReturnDifferentLocks() {
        MachineLocks locks = new MachineLocks();
        assertNotSame(locks.get(UUID.randomUUID()), locks.get(UUID.randomUUID()));
    }

    @Test
    public void removeDropsTheLock() {
        MachineLocks locks = new MachineLocks();
        UUID id = UUID.randomUUID();
        ReentrantLock first = locks.get(id);
        locks.remove(id);
        assertNotSame(first, locks.get(id));
    }
}
