package dev.openrp.fdo.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.model.ActRecord;

/**
 * Derives a member's service sheet from the append-only act log and the duty status. The sheet is the
 * tool of internal-affairs roleplay: a superior with jurisdiction reviews who did what during a
 * shift. The core only aggregates - it draws no automatic consequence from the sheet.
 */
public final class ServiceSheetService {

    private final OpenFdoPlugin plugin;

    public ServiceSheetService(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    public ServiceSheet sheetFor(UUID agent, String agentName) {
        List<ActRecord> acts = plugin.acts().actsBy(agent);
        boolean onDuty = plugin.duty().isOnDuty(agent);
        Optional<Instant> shiftStart = plugin.duty().shiftStart(agent);
        return new ServiceSheet(agent, agentName, onDuty, shiftStart.orElse(null), acts);
    }

    /** Aggregated, presentation-free service sheet data. */
    public record ServiceSheet(UUID agent, String agentName, boolean onDuty, Instant shiftStart, List<ActRecord> acts) {

        public int actCount() {
            return acts.size();
        }
    }
}
