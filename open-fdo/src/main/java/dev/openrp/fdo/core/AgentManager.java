package dev.openrp.fdo.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.config.Corps;
import dev.openrp.fdo.config.FdoConfig;
import dev.openrp.fdo.config.Rank;
import dev.openrp.fdo.model.Agent;

/**
 * The roster of enrolled members and the resolver of their capabilities. Membership is data (corps +
 * rank ids in storage); capabilities are derived from the rank via the config, never stored. Enrol,
 * discharge and rank changes validate against the configured corps/ranks so a member can never hold
 * a rank that does not exist.
 */
public final class AgentManager {

    private final FdoConfig config;
    private final AdapterRegistry adapters;
    private final Counters counters;
    private final Map<UUID, Agent> agents = new LinkedHashMap<>();

    public AgentManager(FdoConfig config, AdapterRegistry adapters, Counters counters) {
        this.config = config;
        this.adapters = adapters;
        this.counters = counters;
    }

    public synchronized void loadAll() {
        agents.clear();
        for (Agent agent : adapters.storage().loadAgents()) {
            agents.put(agent.uuid(), agent);
        }
    }

    public Optional<Agent> agent(UUID uuid) {
        return Optional.ofNullable(uuid == null ? null : agents.get(uuid));
    }

    public boolean isAgent(UUID uuid) {
        return uuid != null && agents.containsKey(uuid);
    }

    public Collection<Agent> all() {
        return Collections.unmodifiableCollection(agents.values());
    }

    /** Corps id of a member, or {@code null}; used as the duty adapter's corps resolver. */
    public String corpsOf(UUID uuid) {
        Agent agent = agents.get(uuid);
        return agent == null ? null : agent.corpsId();
    }

    public Set<Capability> capabilitiesOf(UUID uuid) {
        Agent agent = agents.get(uuid);
        if (agent == null) {
            return Set.of();
        }
        return config.ranks().capabilitiesFor(agent.corpsId(), agent.rankId());
    }

    public boolean has(UUID uuid, Capability capability) {
        return capability != null && capabilitiesOf(uuid).contains(capability);
    }

    /** Whether the member's rank is apical (may enrol and promote within its own corps). */
    public boolean isApical(UUID uuid) {
        Agent agent = agents.get(uuid);
        if (agent == null) {
            return false;
        }
        return config.ranks().rank(agent.corpsId(), agent.rankId()).map(Rank::apical).orElse(false);
    }

    public synchronized FdoResult enroll(UUID uuid, String name, String corpsId, String rankId) {
        if (uuid == null) {
            return FdoResult.fail("agent.enroll_failed");
        }
        if (isAgent(uuid)) {
            return FdoResult.fail("agent.already_enrolled");
        }
        Optional<Corps> corps = config.corps().get(corpsId);
        if (corps.isEmpty()) {
            return FdoResult.fail("agent.unknown_corps", "corps", String.valueOf(corpsId));
        }
        String resolvedRank = rankId;
        if (resolvedRank == null || config.ranks().rank(corpsId, resolvedRank).isEmpty()) {
            // Default to the lowest rank of the corps when none/unknown is supplied.
            Optional<Rank> lowest = config.ranks().lowest(corpsId);
            if (lowest.isEmpty()) {
                return FdoResult.fail("agent.corps_no_ranks", "corps", corpsId);
            }
            resolvedRank = lowest.get().id();
        }
        String matricola = generateMatricola(corps.get());
        Agent agent = new Agent(uuid, name, corpsId, resolvedRank, matricola, System.currentTimeMillis());
        agents.put(uuid, agent);
        adapters.storage().saveAgent(agent);
        return FdoResult.ok("agent.enrolled", "matricola", matricola, "corps", corps.get().displayName())
                .withPayload(agent);
    }

    public synchronized FdoResult discharge(UUID uuid) {
        Agent agent = agents.remove(uuid);
        if (agent == null) {
            return FdoResult.fail("agent.not_enrolled");
        }
        adapters.storage().deleteAgent(uuid);
        return FdoResult.ok("agent.discharged", "name", agent.name());
    }

    public synchronized FdoResult setRank(UUID uuid, String rankId) {
        Agent agent = agents.get(uuid);
        if (agent == null) {
            return FdoResult.fail("agent.not_enrolled");
        }
        if (config.ranks().rank(agent.corpsId(), rankId).isEmpty()) {
            return FdoResult.fail("agent.unknown_rank", "rank", String.valueOf(rankId));
        }
        agent.setRankId(rankId);
        adapters.storage().saveAgent(agent);
        return FdoResult.ok("agent.rank_set", "rank", rankId).withPayload(agent);
    }

    public synchronized FdoResult promote(UUID uuid) {
        Agent agent = agents.get(uuid);
        if (agent == null) {
            return FdoResult.fail("agent.not_enrolled");
        }
        Optional<Rank> next = config.ranks().next(agent.corpsId(), agent.rankId());
        if (next.isEmpty()) {
            return FdoResult.fail("agent.already_top");
        }
        agent.setRankId(next.get().id());
        adapters.storage().saveAgent(agent);
        return FdoResult.ok("agent.promoted", "rank", next.get().displayName()).withPayload(agent);
    }

    public synchronized FdoResult demote(UUID uuid) {
        Agent agent = agents.get(uuid);
        if (agent == null) {
            return FdoResult.fail("agent.not_enrolled");
        }
        Optional<Rank> previous = config.ranks().previous(agent.corpsId(), agent.rankId());
        if (previous.isEmpty()) {
            return FdoResult.fail("agent.already_bottom");
        }
        agent.setRankId(previous.get().id());
        adapters.storage().saveAgent(agent);
        return FdoResult.ok("agent.demoted", "rank", previous.get().displayName()).withPayload(agent);
    }

    private String generateMatricola(Corps corps) {
        long sequence = counters.next("matricola/" + corps.id());
        return corps.sigla() + "-" + String.format("%04d", sequence);
    }

    public List<UUID> agentUuids() {
        return List.copyOf(agents.keySet());
    }
}
