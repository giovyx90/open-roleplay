package dev.openrp.companies.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure banknote arithmetic, separated from any Bukkit item handling so it can be unit-tested. Breaks a
 * whole-unit amount into a greedy count of denominations (largest first) - the same logic used both to
 * dispense cash and to make change. With the default denomination set (which includes {@code 1}) the
 * greedy breakdown is always exact; {@link #remainder} reports any amount the configured denominations
 * cannot represent (only possible if {@code 1} was removed from the set).
 */
public final class Denominations {

    private Denominations() {
    }

    /** Greedy largest-first breakdown of {@code amount} into {@code denom -> count}. {@code denoms} must be descending. */
    public static Map<Integer, Integer> breakdown(long amount, List<Integer> denoms) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        long remaining = Math.max(0L, amount);
        for (int denom : denoms) {
            if (denom <= 0 || remaining < denom) {
                continue;
            }
            long count = remaining / denom;
            out.put(denom, (int) Math.min(count, Integer.MAX_VALUE));
            remaining -= count * denom;
        }
        return out;
    }

    /** The part of {@code amount} the greedy breakdown could not cover (0 when fully representable). */
    public static long remainder(long amount, List<Integer> denoms) {
        long remaining = Math.max(0L, amount);
        for (int denom : denoms) {
            if (denom > 0 && remaining >= denom) {
                remaining %= denom;
            }
        }
        return remaining;
    }

    /** Total value of a {@code denom -> count} breakdown. */
    public static long total(Map<Integer, Integer> breakdown) {
        long sum = 0L;
        for (Map.Entry<Integer, Integer> entry : breakdown.entrySet()) {
            sum += (long) entry.getKey() * entry.getValue();
        }
        return sum;
    }
}
