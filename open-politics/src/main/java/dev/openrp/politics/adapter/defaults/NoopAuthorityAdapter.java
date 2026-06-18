package dev.openrp.politics.adapter.defaults;

import java.util.UUID;
import dev.openrp.politics.adapter.AuthorityAdapter;

/** No-op authority adapter: reports unavailable; emergency declarations are recorded but not relayed. */
public final class NoopAuthorityAdapter implements AuthorityAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void declareEmergency(String governmentId, UUID by, String reason) {
        // No authority backend: nothing to relay.
    }

    @Override
    public void revokeEmergency(String governmentId, UUID by) {
        // No authority backend: nothing to relay.
    }
}
