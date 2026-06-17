package dev.openrp.fdo.adapter;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Reports who is on duty. Replaces the hard dependency on an external badge terminal: a server with a
 * real badge system registers its own implementation, otherwise OpenFDO falls back to a minimal
 * internal duty toggle ({@code /fdo servizio on|off}). Service sheets and escape notifications are
 * built from this.
 */
public interface DutyStatusAdapter {

    String id();

    boolean isOnDuty(UUID agent);

    Set<UUID> onDutyMembers(String corpsId);

    Optional<Instant> shiftStart(UUID agent);
}
