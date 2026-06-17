package dev.openrp.fdo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class DossierTest {

    private Dossier newDossier() {
        return new Dossier("2026/1/PS", UUID.randomUUID(), "Mario", "polizia", UUID.randomUUID(), 1000L);
    }

    @Test
    public void chargesCanBeAddedWhileOpen() {
        Dossier dossier = newDossier();
        assertTrue(dossier.addCharge(new Charge("furto", UUID.randomUUID(), 1L)));
        assertEquals(1, dossier.charges().size());
    }

    @Test
    public void verdictCanBeSignedOnlyOnce() {
        Dossier dossier = newDossier();
        Verdict guilty = new Verdict(VerdictOutcome.GUILTY, 3600L, 1, UUID.randomUUID(), 2000L, "");
        assertTrue(dossier.signVerdict(guilty));
        assertFalse(dossier.signVerdict(new Verdict(VerdictOutcome.ACQUITTED, 0L, 1, UUID.randomUUID(), 3000L, "")));
        assertEquals(VerdictOutcome.GUILTY, dossier.verdict().orElseThrow().outcome());
    }

    @Test
    public void chargesAreRefusedAfterTheVerdictLocksSectionC() {
        Dossier dossier = newDossier();
        dossier.signVerdict(new Verdict(VerdictOutcome.DISMISSED, 0L, 1, UUID.randomUUID(), 2000L, ""));
        assertFalse(dossier.addCharge(new Charge("rapina", UUID.randomUUID(), 4000L)));
        assertTrue(dossier.charges().isEmpty());
    }

    @Test
    public void signingTheVerdictClearsCustodyAndClosesTheFile() {
        Dossier dossier = newDossier();
        dossier.setCustodyDeadline(System.currentTimeMillis() + 100000L);
        assertTrue(dossier.hasActiveCustody());
        dossier.signVerdict(new Verdict(VerdictOutcome.GUILTY, 60L, 1, UUID.randomUUID(), 2000L, ""));
        assertFalse(dossier.hasActiveCustody());
        assertEquals(DossierStatus.CLOSED, dossier.status());
        assertTrue(dossier.isClosed());
    }
}
