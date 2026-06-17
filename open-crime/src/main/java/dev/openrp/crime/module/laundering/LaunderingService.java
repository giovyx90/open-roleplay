package dev.openrp.crime.module.laundering;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.LaunderingMethod;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.core.Ids;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.LaunderingProcess;
import dev.openrp.crime.model.LaunderingStatus;

/**
 * Converts dirty money into clean money over real time, minus the method's loss. The dirty amount is
 * removed from the treasury when the wash starts and the clean amount is credited when it completes;
 * an economic audit can only catch a process while it is still running. Methods whose required adapter
 * is missing are hidden, exactly like an FDO act with an absent adapter.
 */
public final class LaunderingService {

    private final OpenCrimePlugin plugin;
    private final java.util.Map<String, LaunderingProcess> byId = new ConcurrentHashMap<>();

    public LaunderingService(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byId.clear();
        for (LaunderingProcess process : plugin.adapters().storage().loadLaundering()) {
            byId.put(process.id(), process);
        }
    }

    /** Whether laundering can run at all: either no bank is required, or a real bank adapter is active. */
    public boolean bankReady() {
        if (!plugin.config().settings().launderingRequiresBankAdapter()) {
            return true;
        }
        return !plugin.adapters().economy().id().equalsIgnoreCase("internal");
    }

    public boolean adapterAvailable(String requiredAdapter) {
        if (requiredAdapter == null || requiredAdapter.isBlank()) {
            return true;
        }
        return switch (requiredAdapter.toUpperCase(Locale.ROOT)) {
            case "COMPANIES" -> plugin.adapters().company().available();
            case "BANK" -> !plugin.adapters().economy().id().equalsIgnoreCase("internal");
            default -> false;
        };
    }

    public List<LaunderingMethod> availableMethods() {
        List<LaunderingMethod> result = new ArrayList<>();
        for (LaunderingMethod method : plugin.config().laundering().all()) {
            if (adapterAvailable(method.requiresAdapter())) {
                result.add(method);
            }
        }
        return result;
    }

    public CrimeResult start(Player player, String methodId, long amount) {
        if (!bankReady()) {
            return CrimeResult.fail("laundering.no_bank");
        }
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(player.getUniqueId(), Capability.LAUNDER)) {
            return CrimeResult.fail("general.no_capability");
        }
        LaunderingMethod method = plugin.config().laundering().get(methodId).orElse(null);
        if (method == null || !adapterAvailable(method.requiresAdapter())) {
            return CrimeResult.fail("laundering.unknown_method", "method", String.valueOf(methodId));
        }
        if (amount <= 0) {
            return CrimeResult.fail("laundering.bad_amount");
        }
        // Per-DAY cap: sum everything this org has already pushed through this method in the last 24h,
        // so the limit can't be bypassed by splitting it across several concurrent processes.
        if (method.maxPerDay() > 0 && dailyTotal(org.id(), method.id()) + amount > method.maxPerDay()) {
            return CrimeResult.fail("laundering.exceeds_max", "max", String.valueOf(method.maxPerDay()));
        }
        if (!plugin.adapters().economy().withdraw(org.treasury(), amount, true)) {
            return CrimeResult.fail("laundering.insufficient_dirty");
        }

        long started = System.currentTimeMillis();
        long expected = started + plugin.config().settings().realMillisFromHours(method.durationHours());
        LaunderingProcess process = new LaunderingProcess(Ids.prefixed("wash"), org.id(), method.id(),
                amount, started, expected);
        byId.put(process.id(), process);
        plugin.adapters().storage().saveLaundering(process);

        CrimeEvent event = new CrimeEvent(Ids.prefixed("evt"), CrimeEventType.LAUNDERING, org.id(),
                List.of(player.getUniqueId()), List.of(), player.getWorld().getName(),
                player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                player.getLocation().getBlockZ(), started, null);
        plugin.events().register(event);

        long hours = Math.max(1L, plugin.config().settings().realMillisFromHours(method.durationHours()) / 3_600_000L);
        return CrimeResult.ok("laundering.started", "method", method.displayName(),
                "amount", String.valueOf(amount), "hours", String.valueOf(hours));
    }

    /** Settles every elapsed process: credits the clean amount and closes it. Called on a timer. */
    public void settleCompleted() {
        long now = System.currentTimeMillis();
        for (LaunderingProcess process : byId.values()) {
            if (process.status() != LaunderingStatus.ACTIVE || !process.isElapsed(now)) {
                continue;
            }
            LaunderingMethod method = plugin.config().laundering().get(process.methodId()).orElse(null);
            long clean = method == null ? process.amountDirty() : method.cleanFrom(process.amountDirty());
            plugin.orgs().get(process.orgId()).ifPresent(org ->
                    plugin.adapters().economy().deposit(org.treasury(), clean, false));
            process.setAmountClean(clean);
            process.setStatus(LaunderingStatus.COMPLETED);
            process.setCompletedAt(now);
            plugin.adapters().storage().saveLaundering(process);
        }
    }

    public List<LaunderingProcess> activeOfOrg(String orgId) {
        List<LaunderingProcess> result = new ArrayList<>();
        for (LaunderingProcess process : byId.values()) {
            if (process.orgId().equals(orgId) && process.status() == LaunderingStatus.ACTIVE) {
                result.add(process);
            }
        }
        return result;
    }

    public List<LaunderingProcess> ofOrg(String orgId) {
        List<LaunderingProcess> result = new ArrayList<>();
        for (LaunderingProcess process : byId.values()) {
            if (process.orgId().equals(orgId)) {
                result.add(process);
            }
        }
        return result;
    }

    public long remainingMinutes(LaunderingProcess process) {
        long remaining = process.expectedAt() - System.currentTimeMillis();
        return remaining <= 0 ? 0 : Math.max(1L, remaining / 60_000L);
    }

    /** Dirty money this org has pushed through the method in the last 24 real hours. */
    private long dailyTotal(String orgId, String methodId) {
        long since = System.currentTimeMillis() - 86_400_000L;
        long total = 0L;
        for (LaunderingProcess process : byId.values()) {
            if (process.orgId().equals(orgId) && process.methodId().equals(methodId)
                    && process.startedAt() >= since) {
                total += process.amountDirty();
            }
        }
        return total;
    }
}
