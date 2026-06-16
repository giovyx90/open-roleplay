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
 *
 * <p>Locks are intentionally never evicted. Removing a lock that another thread is about to acquire
 * (or already holds) would let a concurrent operation on the same id obtain a <em>different</em> lock
 * instance and run unserialized - the classic delete-vs-create race. The map is bounded by the number
 * of distinct company ids touched in a session (one tiny entry each), so retaining them is cheap and
 * the only way to keep serialization correct.</p>
 */
public final class CompanyLocks {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock get(String companyId) {
        return locks.computeIfAbsent(companyId, ignored -> new ReentrantLock());
    }
}
