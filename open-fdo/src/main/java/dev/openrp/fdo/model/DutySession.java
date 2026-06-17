package dev.openrp.fdo.model;

import java.util.UUID;

/**
 * A single on-duty shift, used by the internal duty fallback and to derive service sheets. A session
 * with {@code shiftEnd == 0} is still ongoing.
 */
public record DutySession(UUID agent, long shiftStart, long shiftEnd) {

    public boolean ongoing() {
        return shiftEnd <= 0L;
    }
}
