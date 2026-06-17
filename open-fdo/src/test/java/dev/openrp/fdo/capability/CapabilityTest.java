package dev.openrp.fdo.capability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CapabilityTest {

    @Test
    public void parsesKnownCapabilityCaseInsensitively() {
        assertEquals(Capability.ARREST, Capability.fromString("arrest").orElseThrow());
        assertEquals(Capability.FLAG_WANTED, Capability.fromString("FLAG_WANTED").orElseThrow());
    }

    @Test
    public void unknownCapabilityIsEmptyNotFatal() {
        assertTrue(Capability.fromString("nonsense").isEmpty());
        assertTrue(Capability.fromString(null).isEmpty());
    }

    @Test
    public void adapterBackedCapabilitiesDeclareTheirAdapter() {
        assertEquals("ECONOMY_AUDIT", Capability.ECONOMIC_AUDIT.requiredAdapter().orElseThrow());
        assertEquals("DETENTION", Capability.MANAGE_DETENTION.requiredAdapter().orElseThrow());
        assertEquals("EXTERNAL_RECORD", Capability.IMPORT_EXTERNAL_RECORD.requiredAdapter().orElseThrow());
    }

    @Test
    public void selfContainedCapabilitiesDeclareNoAdapter() {
        assertFalse(Capability.ARREST.requiredAdapter().isPresent());
        assertFalse(Capability.ISSUE_VERDICT.requiredAdapter().isPresent());
    }
}
