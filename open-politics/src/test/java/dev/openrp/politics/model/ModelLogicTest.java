package dev.openrp.politics.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import dev.openrp.politics.core.ElectionService;

/** Pure-logic tests for the model layer: no Bukkit server, no plugin instance. */
public class ModelLogicTest {

    @Test
    public void lawIsInForceOnlyWithinItsSpan() {
        Law law = new Law("l1", "a1", "comune", "Test", List.of("body"), "civile", 1_000L);
        assertFalse("before enactment", law.wasActiveDuring(500L));
        assertTrue("at enactment", law.wasActiveDuring(1_000L));
        assertTrue("while active", law.wasActiveDuring(5_000L));

        law.setStatus(LawStatus.REPEALED);
        law.setRepealedAt(4_000L);
        assertTrue("a fact before repeal still falls under the law", law.wasActiveDuring(3_999L));
        assertFalse("a fact at/after repeal does not", law.wasActiveDuring(4_000L));
    }

    @Test
    public void holderExpiresOnlyWhenItHasATermThatElapsed() {
        ChargeHolder unlimited = new ChargeHolder("h1", UUID.randomUUID(), "re", "regno", 0L, 0L, "system");
        assertFalse("an unlimited mandate never expires", unlimited.isExpired(Long.MAX_VALUE));

        ChargeHolder termed = new ChargeHolder("h2", UUID.randomUUID(), "sindaco", "comune", 0L, 1_000L, "system");
        assertFalse("not yet", termed.isExpired(999L));
        assertTrue("at the deadline", termed.isExpired(1_000L));
    }

    @Test
    public void electionTallyCountsBallotsPerCandidate() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Election election = new Election("e1", "sindaco", "comune", 1, 0L, 10L, 20L, "system");
        election.addCandidate(a, "Alice");
        election.addCandidate(b, "Bob");
        election.castBallot(UUID.randomUUID(), a);
        election.castBallot(UUID.randomUUID(), a);
        election.castBallot(UUID.randomUUID(), b);

        assertEquals(Integer.valueOf(2), election.tally().get(a));
        assertEquals(Integer.valueOf(1), election.tally().get(b));

        // winners() only reads the election, so it is safe to call with a null plugin here.
        List<UUID> winners = new ElectionService(null).winners(election);
        assertEquals(1, winners.size());
        assertEquals("plurality winner takes the single seat", a, winners.get(0));
    }

    @Test
    public void multiSeatElectionFillsEverySeat() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Election election = new Election("e2", "consiglio", "comune", 2, 0L, 10L, 20L, "system");
        election.addCandidate(a, "A");
        election.addCandidate(b, "B");
        election.addCandidate(c, "C");
        election.castBallot(UUID.randomUUID(), a);
        election.castBallot(UUID.randomUUID(), a);
        election.castBallot(UUID.randomUUID(), b);
        // c gets no votes and must not take a seat.

        List<UUID> winners = new ElectionService(null).winners(election);
        assertEquals(2, winners.size());
        assertTrue(winners.contains(a));
        assertTrue(winners.contains(b));
        assertFalse("a candidate with zero votes wins nothing", winners.contains(c));
    }

    @Test
    public void collegiateVoteCountsChoices() {
        CollegiateVote vote = new CollegiateVote("v1", "a1", "consiglio", 0L, 100L);
        vote.cast(UUID.randomUUID(), BallotChoice.YES);
        vote.cast(UUID.randomUUID(), BallotChoice.YES);
        vote.cast(UUID.randomUUID(), BallotChoice.NO);
        vote.cast(UUID.randomUUID(), BallotChoice.ABSTAIN);

        assertEquals(2, vote.count(BallotChoice.YES));
        assertEquals(1, vote.count(BallotChoice.NO));
        assertEquals(4, vote.castCount());
    }
}
