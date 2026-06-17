package dev.openrp.fdo.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import dev.openrp.fdo.model.DetentionEndReason;
import dev.openrp.fdo.model.DetentionOrder;
import org.junit.Test;

public class AdapterRegistryTest {

    @Test
    public void absentAdaptersGateTheirCapabilities() {
        AdapterRegistry registry = new AdapterRegistry();
        assertFalse(registry.hasAdapter("DETENTION"));
        assertFalse(registry.hasAdapter("ECONOMY_AUDIT"));
        assertFalse(registry.hasAdapter("EXTERNAL_RECORD"));
    }

    @Test
    public void blankRequirementIsAlwaysAvailable() {
        AdapterRegistry registry = new AdapterRegistry();
        assertTrue(registry.hasAdapter(null));
        assertTrue(registry.hasAdapter(""));
    }

    @Test
    public void unknownAdapterIdFailsClosed() {
        // A typo in an act's requires_adapter must hide the act, not silently offer it.
        assertFalse(new AdapterRegistry().hasAdapter("UNKNOWN_ID"));
    }

    @Test
    public void registeringAnAdapterEnablesItsGate() {
        AdapterRegistry registry = new AdapterRegistry();
        registry.setDetention(new DetentionAdapter() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public void beginDetention(DetentionOrder order) {
            }

            @Override
            public void endDetention(UUID inmate, DetentionEndReason reason) {
            }

            @Override
            public boolean isContained(UUID inmate) {
                return true;
            }
        });
        assertTrue(registry.hasAdapter("DETENTION"));
    }
}
