package dev.openrp.vending.core;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.UUID;
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
    public void lockIsRetainedForTheSessionToKeepSerialization() {
        // Locks are intentionally never evicted: a stable instance per id is what keeps a delete and a
        // concurrent same-id operation serialized rather than racing on two instances.
        MachineLocks locks = new MachineLocks();
        UUID id = UUID.randomUUID();
        assertSame(locks.get(id), locks.get(id));
    }
}
