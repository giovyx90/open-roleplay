package dev.openrp.crime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import dev.openrp.crime.capability.Capability;

public class IdsTest {

    @Test
    public void slugifiesNames() {
        assertEquals("cosa_nostra", Ids.slug("Cosa Nostra"));
        assertEquals("clan_dei_corleonesi", Ids.slug("  Clan dei Corleonesi!  "));
        assertTrue(Ids.slug("").matches("[0-9a-f]+"));
    }

    @Test
    public void prefixedIdsAreUnique() {
        assertNotEquals(Ids.prefixed("prod"), Ids.prefixed("prod"));
        assertTrue(Ids.prefixed("evt").startsWith("evt-"));
    }

    @Test
    public void capabilityLookupIsLenient() {
        assertEquals(Capability.PRODUCE, Capability.fromString("produce").orElseThrow());
        assertTrue(Capability.fromString("NOPE").isEmpty());
        assertTrue(Capability.fromString(null).isEmpty());
    }
}
