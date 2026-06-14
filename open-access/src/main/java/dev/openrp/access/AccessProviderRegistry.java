package dev.openrp.access;

import dev.openrp.access.api.OpenAccessOwnerProvider;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccessProviderRegistry {

    private final Map<String, OpenAccessOwnerProvider> providers = new ConcurrentHashMap<>();

    public void register(OpenAccessOwnerProvider provider) {
        if (provider == null || provider.namespace() == null || provider.namespace().isBlank()) {
            return;
        }
        String key = provider.namespace().trim().toLowerCase(Locale.ROOT);
        OpenAccessOwnerProvider previous = providers.put(key, provider);
        if (previous != null && previous != provider) {
            previous.onUnregister();
        }
    }

    public void unregister(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return;
        }
        OpenAccessOwnerProvider removed = providers.remove(namespace.trim().toLowerCase(Locale.ROOT));
        if (removed != null) {
            removed.onUnregister();
        }
    }

    public void clear() {
        providers.values().forEach(OpenAccessOwnerProvider::onUnregister);
        providers.clear();
    }

    public SubjectSnapshot snapshot(AccessProfile profile, UUID playerUuid, String playerName) {
        List<AccessPrincipal> principals = new ArrayList<>();
        boolean manager = false;
        for (OpenAccessOwnerProvider provider : providers.values()) {
            if (!provider.supports(profile)) {
                continue;
            }
            Set<AccessPrincipal> provided = provider.principalsFor(profile, playerUuid, playerName);
            if (provided != null) {
                principals.addAll(provided);
            }
            manager |= provider.canManage(profile, playerUuid, playerName);
        }
        return new SubjectSnapshot(Set.copyOf(principals), manager);
    }

    public record SubjectSnapshot(Set<AccessPrincipal> principals, boolean manager) {
    }
}
