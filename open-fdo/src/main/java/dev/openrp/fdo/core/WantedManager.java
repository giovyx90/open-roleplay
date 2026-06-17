package dev.openrp.fdo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.config.FdoConfig;
import dev.openrp.fdo.model.WantedEntry;

/** The wanted register. Levels are validated against {@code wanted.yml}; the core assumes no scale. */
public final class WantedManager {

    private final FdoConfig config;
    private final AdapterRegistry adapters;
    private final Map<UUID, WantedEntry> bySubject = new LinkedHashMap<>();

    public WantedManager(FdoConfig config, AdapterRegistry adapters) {
        this.config = config;
        this.adapters = adapters;
    }

    public synchronized void loadAll() {
        bySubject.clear();
        for (WantedEntry entry : adapters.storage().loadWanted()) {
            bySubject.put(entry.subjectUuid(), entry);
        }
    }

    public Optional<WantedEntry> find(UUID subject) {
        return Optional.ofNullable(subject == null ? null : bySubject.get(subject));
    }

    public boolean isWanted(UUID subject) {
        WantedEntry entry = subject == null ? null : bySubject.get(subject);
        return entry != null && entry.active();
    }

    public Collection<WantedEntry> all() {
        return Collections.unmodifiableCollection(bySubject.values());
    }

    public List<WantedEntry> active() {
        List<WantedEntry> result = new ArrayList<>();
        for (WantedEntry entry : bySubject.values()) {
            if (entry.active()) {
                result.add(entry);
            }
        }
        return result;
    }

    public synchronized FdoResult flag(UUID subject, String name, int level, String reason, UUID by) {
        if (subject == null) {
            return FdoResult.fail("wanted.no_subject");
        }
        if (!config.wanted().exists(level)) {
            return FdoResult.fail("wanted.unknown_level", "level", level);
        }
        WantedEntry entry = bySubject.get(subject);
        if (entry == null) {
            entry = new WantedEntry(subject, name, level, reason, by, System.currentTimeMillis());
            bySubject.put(subject, entry);
        } else {
            entry.setLevel(level);
            entry.setReason(reason);
            entry.setActive(true);
        }
        adapters.storage().saveWanted(entry);
        return FdoResult.ok("wanted.flagged", "name", entry.subjectName(), "level", level).withPayload(entry);
    }

    public synchronized FdoResult clear(UUID subject) {
        WantedEntry entry = bySubject.get(subject);
        if (entry == null || !entry.active()) {
            return FdoResult.fail("wanted.not_wanted");
        }
        entry.setActive(false);
        adapters.storage().saveWanted(entry);
        return FdoResult.ok("wanted.cleared", "name", entry.subjectName()).withPayload(entry);
    }
}
