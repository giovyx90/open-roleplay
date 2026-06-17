package dev.openrp.fdo.core;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.adapter.defaults.InternalDutyStatusAdapter;
import dev.openrp.fdo.config.FdoConfig;
import dev.openrp.fdo.model.DutySession;

/**
 * Thin layer over whatever {@code DutyStatusAdapter} is active. Clock-in/out only act when the
 * internal fallback is in use; with a real external badge system OpenFDO defers to it. Completed
 * shifts are appended to the append-only duty log so service sheets can be derived later.
 */
public final class DutyService {

    private final FdoConfig config;
    private final AdapterRegistry adapters;

    public DutyService(FdoConfig config, AdapterRegistry adapters) {
        this.config = config;
        this.adapters = adapters;
    }

    public boolean isOnDuty(UUID agent) {
        return adapters.duty().isOnDuty(agent);
    }

    public Optional<Instant> shiftStart(UUID agent) {
        return adapters.duty().shiftStart(agent);
    }

    /** Every member on duty across every configured corps. */
    public Set<UUID> onDutyEverywhere() {
        Set<UUID> result = new LinkedHashSet<>();
        for (String corpsId : config.corps().ids()) {
            result.addAll(adapters.duty().onDutyMembers(corpsId));
        }
        return result;
    }

    public FdoResult clockIn(UUID agent) {
        if (!(adapters.duty() instanceof InternalDutyStatusAdapter internal)) {
            return FdoResult.fail("duty.external_managed");
        }
        if (!internal.clockIn(agent, Instant.now())) {
            return FdoResult.fail("duty.already_on");
        }
        return FdoResult.ok("duty.clocked_in");
    }

    public FdoResult clockOut(UUID agent) {
        if (!(adapters.duty() instanceof InternalDutyStatusAdapter internal)) {
            return FdoResult.fail("duty.external_managed");
        }
        Optional<Instant> start = internal.clockOut(agent);
        if (start.isEmpty()) {
            return FdoResult.fail("duty.already_off");
        }
        adapters.storage().appendDutySession(
                new DutySession(agent, start.get().toEpochMilli(), System.currentTimeMillis()));
        return FdoResult.ok("duty.clocked_out");
    }
}
