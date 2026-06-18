package dev.openrp.politics.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import dev.openrp.politics.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.politics.model.ActStatus;
import dev.openrp.politics.model.BallotChoice;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.CollegiateVote;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/** The in-memory adapter is the storage contract reference; verify each entity round-trips. */
public class MemoryStorageRoundTripTest {

    @Test
    public void holdersRoundTrip() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        storage.init();
        ChargeHolder holder = new ChargeHolder("h1", UUID.randomUUID(), "sindaco", "comune", 1L, 2L, "system");
        storage.saveHolder(holder);
        assertEquals(1, storage.loadHolders().size());
        storage.deleteHolder("h1");
        assertTrue(storage.loadHolders().isEmpty());
    }

    @Test
    public void electionKeepsCandidaciesAndBallots() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        Election election = new Election("e1", "sindaco", "comune", 1, 0L, 1L, 2L, "system");
        UUID candidate = UUID.randomUUID();
        election.addCandidate(candidate, "Alice");
        election.castBallot(UUID.randomUUID(), candidate);
        storage.saveElection(election);

        Election loaded = storage.loadElections().iterator().next();
        assertEquals(1, loaded.candidateCount());
        assertEquals(1, loaded.ballotCount());
    }

    @Test
    public void actAndLawRoundTrip() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        PoliticalAct act = new PoliticalAct("a1", "2026/1/COM", "decreto", "comune", UUID.randomUUID(),
                "sindaco", "Titolo", List.of("riga"), 10L);
        act.setStatus(ActStatus.SIGNED);
        storage.saveAct(act);
        assertEquals("2026/1/COM", storage.loadActs().iterator().next().displayId());

        Law law = new Law("l1", "a1", "comune", "Legge", List.of("corpo"), "civile", 11L);
        storage.saveLaw(law);
        assertTrue(storage.loadLaws().iterator().next().isActive());
    }

    @Test
    public void collegiateVoteRoundTrip() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        CollegiateVote vote = new CollegiateVote("v1", "a1", "consiglio", 0L, 100L);
        vote.cast(UUID.randomUUID(), BallotChoice.YES);
        storage.saveCollegiateVote(vote);
        assertEquals(1, storage.loadCollegiateVotes().iterator().next().castCount());
    }

    @Test
    public void governmentStateAndCountersPersist() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        storage.saveGovernmentState("comune", false);
        assertEquals(Boolean.FALSE, storage.loadGovernmentStates().get("comune"));
        storage.saveCounter("act_seq_comune_2026", 7L);
        assertEquals(Long.valueOf(7L), storage.loadCounters().get("act_seq_comune_2026"));
    }
}
