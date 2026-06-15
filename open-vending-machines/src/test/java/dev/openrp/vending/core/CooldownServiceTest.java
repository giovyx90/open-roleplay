package dev.openrp.vending.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;

public class CooldownServiceTest {

    @Test
    public void zeroCooldownNeverBlocks() {
        CooldownService cooldowns = new CooldownService();
        UUID id = UUID.randomUUID();
        cooldowns.mark(id);
        assertFalse(cooldowns.isOnCooldown(id, 0));
    }

    @Test
    public void marksChecksAndClears() {
        CooldownService cooldowns = new CooldownService();
        UUID id = UUID.randomUUID();
        assertFalse(cooldowns.isOnCooldown(id, 10_000));
        cooldowns.mark(id);
        assertTrue(cooldowns.isOnCooldown(id, 10_000));
        assertFalse(cooldowns.isOnCooldown(UUID.randomUUID(), 10_000));
        cooldowns.clear(id);
        assertFalse(cooldowns.isOnCooldown(id, 10_000));
    }
}
