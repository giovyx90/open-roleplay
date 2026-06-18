package dev.openrp.politics.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An election for a charge. The plugin owns the whole mechanism: opening and closing the campaign and
 * the vote, registering candidacies, collecting one ballot per voter and computing the winner(s). The
 * voter→candidate map is the only sensitive record - it is never exposed when anonymous voting is on;
 * the public side only ever sees per-candidate tallies. Bukkit-free, mutated under the manager lock.
 */
public final class Election {

    private final String id;
    private final String chargeId;
    private final String governmentId;
    private final int seats;
    private ElectionStatus status = ElectionStatus.CAMPAIGN;
    private final long campaignStart;
    private long votingStart;
    private long votingEnd;
    private final String calledBy;
    // candidate uuid -> display name
    private final Map<UUID, String> candidacies = new LinkedHashMap<>();
    // voter uuid -> chosen candidate uuid (kept private; enforces one ballot per voter)
    private final Map<UUID, UUID> ballots = new LinkedHashMap<>();

    public Election(String id, String chargeId, String governmentId, int seats, long campaignStart,
                    long votingStart, long votingEnd, String calledBy) {
        this.id = Objects.requireNonNull(id, "id");
        this.chargeId = Objects.requireNonNull(chargeId, "chargeId");
        this.governmentId = governmentId == null ? "" : governmentId;
        this.seats = Math.max(1, seats);
        this.campaignStart = campaignStart;
        this.votingStart = votingStart;
        this.votingEnd = votingEnd;
        this.calledBy = calledBy == null ? "system" : calledBy;
    }

    public String id() {
        return id;
    }

    public String chargeId() {
        return chargeId;
    }

    public String governmentId() {
        return governmentId;
    }

    public int seats() {
        return seats;
    }

    public ElectionStatus status() {
        return status;
    }

    public void setStatus(ElectionStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public long campaignStart() {
        return campaignStart;
    }

    public long votingStart() {
        return votingStart;
    }

    public void setVotingStart(long votingStart) {
        this.votingStart = votingStart;
    }

    public long votingEnd() {
        return votingEnd;
    }

    public void setVotingEnd(long votingEnd) {
        this.votingEnd = votingEnd;
    }

    public String calledBy() {
        return calledBy;
    }

    // --- candidacies -------------------------------------------------------------------------

    public Map<UUID, String> candidacies() {
        return Collections.unmodifiableMap(candidacies);
    }

    public boolean isCandidate(UUID player) {
        return player != null && candidacies.containsKey(player);
    }

    public void addCandidate(UUID player, String name) {
        candidacies.put(player, name == null ? "" : name);
    }

    public int candidateCount() {
        return candidacies.size();
    }

    // --- ballots -----------------------------------------------------------------------------

    public boolean hasVoted(UUID voter) {
        return voter != null && ballots.containsKey(voter);
    }

    public void castBallot(UUID voter, UUID candidate) {
        ballots.put(voter, candidate);
    }

    /** Raw ballots - storage only. Never expose this through the public API when anonymity is on. */
    public Map<UUID, UUID> ballots() {
        return Collections.unmodifiableMap(ballots);
    }

    public int ballotCount() {
        return ballots.size();
    }

    /** Per-candidate vote counts, the only public projection of the ballots. */
    public Map<UUID, Integer> tally() {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (UUID candidate : candidacies.keySet()) {
            counts.put(candidate, 0);
        }
        for (UUID candidate : ballots.values()) {
            counts.merge(candidate, 1, Integer::sum);
        }
        return counts;
    }
}
