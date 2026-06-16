package dev.openrp.companies.core;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.concurrent.locks.ReentrantLock;
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
    public void removeDropsTheLock() {
        CompanyLocks locks = new CompanyLocks();
        ReentrantLock first = locks.get("acme");
        locks.remove("acme");
        assertNotSame(first, locks.get("acme"));
    }
}
