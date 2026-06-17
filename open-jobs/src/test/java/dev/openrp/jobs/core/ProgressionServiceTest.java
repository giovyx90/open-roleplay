package dev.openrp.jobs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.jobs.config.ProgressionLadder;
import dev.openrp.jobs.model.WorkRecord;

public class ProgressionServiceTest {

    private static final long DAY = 86_400_000L;
    private static final long NOW = 1_700_000_000_000L;

    private ProgressionLadder ladder(boolean decay) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
                "progression:",
                "  tiers:",
                "    - id: novizio",
                "      sessions_required: 0",
                "      pay_multiplier: 1.0",
                "    - id: lavoratore",
                "      sessions_required: 60",
                "      pay_multiplier: 1.2",
                "    - id: esperto",
                "      sessions_required: 150",
                "      pay_multiplier: 1.35",
                "  decay:",
                "    enabled: " + decay,
                "    inactive_days_threshold: 45",
                "    decay_sessions_per_day: 0.5"));
        ProgressionLadder ladder = new ProgressionLadder();
        ladder.load(config.getConfigurationSection("progression"));
        return ladder;
    }

    private WorkRecord record(int sessions, long lastSessionAt) {
        WorkRecord record = new WorkRecord(UUID.randomUUID(), "minatore");
        record.setTotalSessions(sessions);
        record.setLastSessionAt(lastSessionAt);
        return record;
    }

    @Test
    public void tierDerivedFromSessions() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(false));
        assertEquals("novizio", service.currentTier(record(10, NOW), NOW).orElseThrow().id());
        assertEquals("lavoratore", service.currentTier(record(60, NOW), NOW).orElseThrow().id());
        assertEquals("esperto", service.currentTier(record(155, NOW), NOW).orElseThrow().id());
    }

    @Test
    public void inactivityDecayErodesTier() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(true));
        WorkRecord record = record(155, NOW - 65 * DAY); // 20 days over threshold -> 10 sessions decayed
        assertEquals(10.0, service.decayFor(record, NOW), 0.0001);
        assertEquals(145.0, service.effectiveSessions(record, NOW), 0.0001);
        // 145 effective < 150, so the master drops to lavoratore.
        assertEquals("lavoratore", service.currentTier(record, NOW).orElseThrow().id());
    }

    @Test
    public void recentWorkerDoesNotDecay() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(true));
        WorkRecord record = record(155, NOW - 10 * DAY);
        assertEquals(0.0, service.decayFor(record, NOW), 0.0001);
        assertEquals("esperto", service.currentTier(record, NOW).orElseThrow().id());
    }

    @Test
    public void refreshTierReportsChange() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(false));
        WorkRecord record = record(60, NOW);
        record.setCurrentTier("novizio");
        assertTrue(service.refreshTier(record, NOW));
        assertEquals("lavoratore", record.currentTier());
    }

    @Test
    public void sessionsToNextTier() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(false));
        assertEquals(Integer.valueOf(50), service.sessionsToNextTier(record(10, NOW), NOW).orElseThrow());
        assertTrue(service.sessionsToNextTier(record(400, NOW), NOW).isEmpty());
    }

    @Test
    public void payMultiplierFollowsTier() throws InvalidConfigurationException {
        ProgressionService service = new ProgressionService(ladder(false));
        YamlConfiguration jobConfig = new YamlConfiguration();
        jobConfig.loadFromString(String.join("\n",
                "job:",
                "  location_type: mine",
                "  progression_enabled: true"));
        dev.openrp.jobs.config.Job job = dev.openrp.jobs.config.Job.from("job", jobConfig.getConfigurationSection("job"));
        assertEquals(1.35, service.payMultiplier(record(150, NOW), job, NOW), 0.0001);
    }
}
