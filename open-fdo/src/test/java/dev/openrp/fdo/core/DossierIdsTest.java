package dev.openrp.fdo.core;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import org.junit.Test;
import dev.openrp.fdo.config.Corps;

public class DossierIdsTest {

    @Test
    public void formatsDefaultPattern() {
        Corps corps = new Corps("polizia", "Polizia di Stato", "PS", Set.of());
        assertEquals("2026/7/PS", DossierIds.format("{anno}/{numero}/{sigla_corpo}", 2026, 7, corps));
    }

    @Test
    public void formatsCustomPattern() {
        Corps corps = new Corps("guard", "City Guard", "CG", Set.of());
        assertEquals("CG-2026-12", DossierIds.format("{sigla_corpo}-{anno}-{numero}", 2026, 12, corps));
    }

    @Test
    public void counterKeyCombinesYearAndCorps() {
        assertEquals("2026/polizia", DossierIds.counterKey(2026, "polizia"));
    }
}
