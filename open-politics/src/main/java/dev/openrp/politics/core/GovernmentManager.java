package dev.openrp.politics.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.adapter.AdapterRegistry;
import dev.openrp.politics.config.Government;
import dev.openrp.politics.config.GovernmentCatalog;

/**
 * Owns the activation state of governments. The <em>definition</em> of a government is config
 * ({@link GovernmentCatalog}); whether it is currently active starts from the config default and can be
 * flipped by an admin, with the override persisted so it survives a reload. Activating or deactivating
 * a government never deletes anything - it only changes what the commands and the API surface.
 */
public final class GovernmentManager {

    private final GovernmentCatalog catalog;
    private final AdapterRegistry adapters;
    private final Map<String, Boolean> overrides = new ConcurrentHashMap<>();

    public GovernmentManager(GovernmentCatalog catalog, AdapterRegistry adapters) {
        this.catalog = catalog;
        this.adapters = adapters;
    }

    public void loadAll() {
        overrides.clear();
        overrides.putAll(adapters.storage().loadGovernmentStates());
    }

    public Optional<Government> get(String governmentId) {
        return catalog.get(governmentId);
    }

    public boolean exists(String governmentId) {
        return catalog.exists(governmentId);
    }

    public boolean isActive(String governmentId) {
        Optional<Government> government = catalog.get(governmentId);
        if (government.isEmpty()) {
            return false;
        }
        Boolean override = overrides.get(governmentId);
        return override != null ? override : government.get().activeByDefault();
    }

    /** Flips a government's active state and persists the override. No-op for an unknown government. */
    public synchronized PoliticsResult setActive(String governmentId, boolean active) {
        Optional<Government> government = catalog.get(governmentId);
        if (government.isEmpty()) {
            return PoliticsResult.fail("government.unknown", "id", governmentId);
        }
        overrides.put(governmentId, active);
        adapters.storage().saveGovernmentState(governmentId, active);
        return PoliticsResult.ok(active ? "government.activated" : "government.deactivated",
                "name", government.get().displayName());
    }

    public List<Government> activeGovernments() {
        return catalog.all().stream().filter(g -> isActive(g.id())).toList();
    }

    public List<Government> all() {
        return List.copyOf(catalog.all());
    }
}
