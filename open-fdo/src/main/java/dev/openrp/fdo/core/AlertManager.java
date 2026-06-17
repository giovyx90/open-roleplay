package dev.openrp.fdo.core;

import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.model.AlertState;

/** Holds the single server-wide alert state, persisted across restarts. */
public final class AlertManager {

    private final AdapterRegistry adapters;
    private AlertState state = AlertState.none();

    public AlertManager(AdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public void loadAll() {
        this.state = adapters.storage().loadAlert().orElse(AlertState.none());
    }

    public AlertState current() {
        return state;
    }

    public synchronized FdoResult declare(int level, String reason, UUID by) {
        if (level <= 0) {
            return FdoResult.fail("alert.invalid_level");
        }
        this.state = new AlertState(level, reason == null ? "" : reason, by, System.currentTimeMillis());
        adapters.storage().saveAlert(state);
        return FdoResult.ok("alert.declared", "level", level);
    }

    public synchronized FdoResult clear() {
        if (!state.active()) {
            return FdoResult.fail("alert.none");
        }
        this.state = AlertState.none();
        adapters.storage().saveAlert(state);
        return FdoResult.ok("alert.cleared");
    }
}
