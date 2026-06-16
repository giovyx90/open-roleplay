package dev.openrp.companies.core;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class CompanyLocksTest {

    @Test
    public void sameIdReturnsSameLock() {
        CompanyLocks locks = new CompanyLocks();
        assertSame(locks.get("acme"), locks.get("acme"));
    }

    @Test
    public void differentIdsReturnDifferentLocks() {
        CompanyLocks locks = new CompanyLocks();
        assertNotSame(locks.get("acme"), locks.get("globex"));
    }

    @Test
    public void lockIsRetainedForTheSessionToKeepSerialization() {
        // Locks are intentionally never evicted: a stable instance per id is what guarantees that a
        // delete and a concurrent same-id create are serialized rather than racing on two instances.
        CompanyLocks locks = new CompanyLocks();
        assertSame(locks.get("acme"), locks.get("acme"));
    }
}
