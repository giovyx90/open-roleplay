package dev.openrp.companies.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-company reentrant locks. Every operation that mutates a company's members, roles, balance,
 * status, licenses, headquarters or assets runs inside that company's lock, so two actors can never
 * interleave on the same company and corrupt its state or duplicate money/assets. Locks are
 * reentrant, so a service already holding a company's lock can safely call another locked method on
 * the same thread.
 */
public final class CompanyLocks {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock get(String companyId) {
        return locks.computeIfAbsent(companyId, ignored -> new ReentrantLock());
    }

    public void remove(String companyId) {
        locks.remove(companyId);
    }
}
