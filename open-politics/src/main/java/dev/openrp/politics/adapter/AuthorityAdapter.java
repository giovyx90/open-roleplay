package dev.openrp.politics.adapter;

import java.util.UUID;

/**
 * Bridge to the authority backend (e.g. Open FDO). The connection is two-way but only this direction
 * needs an adapter: Open Politics tells the authorities when a {@code DECLARE_EMERGENCY} holder raises
 * or lifts the state of emergency. The reverse direction - the authorities reading the active law
 * registry - goes through {@link dev.openrp.politics.api.OpenPoliticsApi}. The default is a no-op whose
 * {@link #available()} reports {@code false}.
 */
public interface AuthorityAdapter {

    String id();

    boolean available();

    /** A charge holder with DECLARE_EMERGENCY has raised the state of emergency. */
    void declareEmergency(String governmentId, UUID by, String reason);

    /** A charge holder with DECLARE_EMERGENCY has lifted the state of emergency. */
    void revokeEmergency(String governmentId, UUID by);
}
