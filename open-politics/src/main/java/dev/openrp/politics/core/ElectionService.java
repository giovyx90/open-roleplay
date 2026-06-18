package dev.openrp.politics.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.AssignmentMechanism;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.ElectionStatus;

/**
 * Runs the whole election mechanism: opening the campaign, registering candidacies, collecting one
 * ballot per voter, and - when the voting window closes - computing the winner(s) and assigning the
 * charge through the {@link ChargeManager}. This is one of the two mechanisms where the plugin assigns
 * a charge automatically (the other is conquest); everything else is signed by a holder. Ballots are
 * never exposed: only per-candidate tallies, and only once the election is closed.
 */
public final class ElectionService {

    private final OpenPoliticsPlugin plugin;
    private final Map<String, Election> elections = new ConcurrentHashMap<>();

    public ElectionService(OpenPoliticsPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void loadAll() {
        elections.clear();
        for (Election election : plugin.adapters().storage().loadElections()) {
            elections.put(election.id(), election);
        }
    }

    // --- lookups -----------------------------------------------------------------------------

    public Optional<Election> get(String id) {
        return Optional.ofNullable(id == null ? null : elections.get(id));
    }

    public List<Election> all() {
        return new ArrayList<>(elections.values());
    }

    public List<Election> open() {
        return elections.values().stream()
                .filter(e -> e.status() == ElectionStatus.CAMPAIGN || e.status() == ElectionStatus.VOTING)
                .toList();
    }

    public Optional<Election> openForCharge(String chargeId) {
        return open().stream().filter(e -> e.chargeId().equals(chargeId)).findFirst();
    }

    // --- calling an election -----------------------------------------------------------------

    public synchronized PoliticsResult call(UUID actor, boolean admin, String chargeId) {
        ChargeDef charge = plugin.config().charges().get(chargeId).orElse(null);
        if (charge == null) {
            return PoliticsResult.fail("charge.unknown", "id", chargeId);
        }
        if (!charge.mechanism().is(AssignmentMechanism.ELECTION) && !admin) {
            return PoliticsResult.fail("election.not_elective", "charge", charge.displayName());
        }
        if (!admin && !plugin.charges().hasInGovernment(actor, PoliticalCapability.CALL_ELECTION, charge.governmentId())) {
            return PoliticsResult.fail("general.no_capability");
        }
        if (openForCharge(chargeId).isPresent()) {
            return PoliticsResult.fail("election.already_open", "charge", charge.displayName());
        }
        AssignmentMechanism mechanism = charge.mechanism();
        long now = System.currentTimeMillis();
        long votingStart = now + plugin.config().settings()
                .realMillisFromDays(mechanism.integer("campaign_duration_days", 3));
        long votingEnd = votingStart + plugin.config().settings()
                .realMillisFromDays(mechanism.integer("voting_duration_days", 1));
        Election election = new Election(Ids.prefixed("elec"), chargeId, charge.governmentId(),
                charge.maxHolders(), now, votingStart, votingEnd, actor == null ? "admin" : actor.toString());
        elections.put(election.id(), election);
        plugin.adapters().storage().saveElection(election);
        if (plugin.config().settings().electionsAnnounceOpening()) {
            broadcast("election.announce_open", "charge", charge.displayName(), "id", election.id());
        }
        return PoliticsResult.ok("election.called", "charge", charge.displayName(), "id", election.id())
                .withPayload(election);
    }

    // --- candidacy ---------------------------------------------------------------------------

    public synchronized PoliticsResult candidacy(UUID player, String playerName, String electionId) {
        Election election = elections.get(electionId);
        if (election == null) {
            return PoliticsResult.fail("election.unknown", "id", electionId);
        }
        if (election.status() != ElectionStatus.CAMPAIGN) {
            return PoliticsResult.fail("election.not_campaign");
        }
        if (election.isCandidate(player)) {
            return PoliticsResult.fail("election.already_candidate");
        }
        election.addCandidate(player, playerName);
        plugin.adapters().storage().saveElection(election);
        return PoliticsResult.ok("election.candidacy_registered", "id", electionId).withPayload(election);
    }

    // --- voting ------------------------------------------------------------------------------

    public synchronized PoliticsResult vote(UUID voter, String electionId, UUID candidate) {
        Election election = elections.get(electionId);
        if (election == null) {
            return PoliticsResult.fail("election.unknown", "id", electionId);
        }
        if (election.status() != ElectionStatus.VOTING) {
            return PoliticsResult.fail("election.not_voting");
        }
        if (candidate == null || !election.isCandidate(candidate)) {
            return PoliticsResult.fail("election.not_a_candidate");
        }
        if (election.hasVoted(voter)) {
            return PoliticsResult.fail("election.already_voted");
        }
        election.castBallot(voter, candidate);
        plugin.adapters().storage().saveElection(election);
        return PoliticsResult.ok("election.vote_cast").withPayload(election);
    }

    public synchronized PoliticsResult cancel(String electionId) {
        Election election = elections.get(electionId);
        if (election == null) {
            return PoliticsResult.fail("election.unknown", "id", electionId);
        }
        election.setStatus(ElectionStatus.CANCELLED);
        plugin.adapters().storage().saveElection(election);
        return PoliticsResult.ok("election.cancelled", "id", electionId).withPayload(election);
    }

    // --- lifecycle ---------------------------------------------------------------------------

    /** Advances every open election: campaign → voting, then voting → closed with assignment. */
    public synchronized void tick(long now) {
        for (Election election : new ArrayList<>(elections.values())) {
            if (election.status() == ElectionStatus.CAMPAIGN && now >= election.votingStart()) {
                openVoting(election, now);
            }
            if (election.status() == ElectionStatus.VOTING && now >= election.votingEnd()) {
                close(election);
            }
        }
    }

    private void openVoting(Election election, long now) {
        ChargeDef charge = plugin.config().charges().get(election.chargeId()).orElse(null);
        int minCandidates = charge == null ? 0 : charge.mechanism().integer("min_candidates", 0);
        if (election.candidateCount() < Math.max(1, minCandidates)) {
            // Too few candidates to hold a meaningful vote: cancel and leave the charge as it is.
            election.setStatus(ElectionStatus.CANCELLED);
            plugin.adapters().storage().saveElection(election);
            broadcast("election.cancelled_no_candidates", "id", election.id());
            return;
        }
        election.setStatus(ElectionStatus.VOTING);
        plugin.adapters().storage().saveElection(election);
        broadcast("election.announce_voting", "id", election.id());
    }

    private void close(Election election) {
        election.setStatus(ElectionStatus.CLOSED);
        plugin.adapters().storage().saveElection(election);
        List<UUID> winners = winners(election);
        for (UUID winner : winners) {
            plugin.charges().assign(winner, election.chargeId(), "election", true);
        }
        if (plugin.config().settings().electionsAnnounceResults()) {
            ChargeDef charge = plugin.config().charges().get(election.chargeId()).orElse(null);
            broadcast("election.announce_results",
                    "charge", charge == null ? election.chargeId() : charge.displayName(),
                    "winners", String.valueOf(winners.size()), "id", election.id());
        }
    }

    /**
     * The winning candidate(s): the top {@code seats} by vote count. Single-seat charges thus resolve
     * by plurality; a collegiate body fills every seat. {@code majority}/{@code runoff} configs are
     * accepted but resolved as plurality in the core - a runoff round is an RP escalation, not a
     * mechanic the plugin forces.
     */
    public List<UUID> winners(Election election) {
        Map<UUID, Integer> tally = election.tally();
        return tally.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey(), Comparator.naturalOrder()))
                .limit(election.seats())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void broadcast(String key, Object... placeholders) {
        plugin.adapters().notification().broadcast(
                plugin.messages().mini(plugin.getServer().getConsoleSender(), key, placeholders));
    }
}
