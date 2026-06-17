package dev.openrp.fdo.adapter.defaults;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import dev.openrp.fdo.adapter.DutyStatusAdapter;

/**
 * Minimal internal duty fallback used when no external badge system registers a
 * {@link DutyStatusAdapter}. Members toggle their shift with {@code /fdo servizio on|off}. State is
 * in-memory: a restart clears it, which is the sane default (nobody is on duty until they clock in).
 * Corps membership is resolved through an injected function so this adapter stays decoupled from the
 * agent store.
 */
public final class InternalDutyStatusAdapter implements DutyStatusAdapter {

    private final Map<UUID, Instant> shifts = new ConcurrentHashMap<>();
    private final Function<UUID, String> corpsResolver;

    public InternalDutyStatusAdapter(Function<UUID, String> corpsResolver) {
        this.corpsResolver = corpsResolver == null ? uuid -> null : corpsResolver;
    }

    @Override
    public String id() {
        return "internal";
    }

    @Override
    public boolean isOnDuty(UUID agent) {
        return agent != null && shifts.containsKey(agent);
    }

    @Override
    public Set<UUID> onDutyMembers(String corpsId) {
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID agent : shifts.keySet()) {
            if (corpsId == null || corpsId.equals(corpsResolver.apply(agent))) {
                result.add(agent);
            }
        }
        return result;
    }

    @Override
    public Optional<Instant> shiftStart(UUID agent) {
        return Optional.ofNullable(agent == null ? null : shifts.get(agent));
    }

    /** Clocks the member in. Returns {@code false} if they were already on duty. */
    public boolean clockIn(UUID agent, Instant when) {
        return shifts.putIfAbsent(agent, when == null ? Instant.now() : when) == null;
    }

    /** Clocks the member out, returning the shift start if they were on duty. */
    public Optional<Instant> clockOut(UUID agent) {
        return Optional.ofNullable(shifts.remove(agent));
    }

    /** Every member currently on duty, across all corps. */
    public Set<UUID> allOnDuty() {
        return new LinkedHashSet<>(shifts.keySet());
    }
}
