package dev.openrp.jobs.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.jobs.model.ActivityDetection;
import dev.openrp.jobs.model.PaymentModel;
import dev.openrp.jobs.model.Season;

public class ConfigParsingTest {

    private YamlConfiguration yaml(String body) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(body);
        return config;
    }

    @Test
    public void jobParsesEveryLayer() throws InvalidConfigurationException {
        YamlConfiguration config = yaml(String.join("\n",
                "minatore:",
                "  display_name: Minatore",
                "  category: estrattivo",
                "  location_type: mine",
                "  payment_model: a_produzione",
                "  requires_license: true",
                "  license:",
                "    auto_issue: true",
                "  required_tool:",
                "    enabled: true",
                "    material: WOODEN_PICKAXE",
                "    bonus_materials:",
                "      DIAMOND_PICKAXE: 1.1",
                "  cooperative:",
                "    enabled: true",
                "    min_players: 2",
                "  payment:",
                "    minimum_payout: 5.0",
                "    rates:",
                "      IRON_ORE: 5.0",
                "      STONE: 0.3"));
        Job job = Job.from("minatore", config.getConfigurationSection("minatore"));
        assertEquals("Minatore", job.displayName());
        assertEquals(PaymentModel.A_PRODUZIONE, job.paymentModel());
        assertTrue(job.requiresLicense());
        assertTrue(job.license().autoIssue());
        assertTrue(job.tool().enabled());
        assertEquals(1.1, job.tool().bonusFor("DIAMOND_PICKAXE"), 0.0001);
        assertEquals(1.0, job.tool().bonusFor("STONE_PICKAXE"), 0.0001);
        assertEquals(5.0, job.payment().rate("IRON_ORE"), 0.0001);
        assertEquals(5.0, job.payment().rate("iron_ore"), 0.0001); // case-insensitive lookup
        assertEquals(0.0, job.payment().rate("iron"), 0.0001);     // unknown material -> not paid
        assertEquals(5.0, job.payment().minimumPayout(), 0.0001);
        assertFalse(job.isTransformative());
    }

    @Test
    public void transformativeJobParsesRecipes() throws InvalidConfigurationException {
        YamlConfiguration config = yaml(String.join("\n",
                "falegname:",
                "  location_type: workshop",
                "  payment_model: a_produzione",
                "  transformations:",
                "    - input:",
                "        - material: OAK_LOG",
                "          amount: 1",
                "      output:",
                "        - material: OAK_PLANKS",
                "          amount: 4",
                "      payout: 2.0",
                "      craft_time_seconds: 10"));
        Job job = Job.from("falegname", config.getConfigurationSection("falegname"));
        assertTrue(job.isTransformative());
        assertEquals(1, job.transformations().size());
        Transformation transformation = job.transformations().get(0);
        assertEquals("OAK_PLANKS", transformation.outputs().get(0).material());
        assertEquals(4, transformation.outputs().get(0).amount());
        assertEquals(2.0, transformation.payout(), 0.0001);
        assertEquals(10, transformation.craftTimeSeconds());
    }

    @Test
    public void locationTypeDetectionAndMaterials() throws InvalidConfigurationException {
        YamlConfiguration config = yaml(String.join("\n",
                "mine:",
                "  display_name: Miniera",
                "  activity_detection: block_break",
                "  region_tag: job_mine",
                "  valid_materials:",
                "    - STONE",
                "    - IRON_ORE"));
        LocationType type = LocationType.from("mine", config.getConfigurationSection("mine"));
        assertEquals(ActivityDetection.BLOCK_BREAK, type.activityDetection());
        assertTrue(type.accepts("stone"));
        assertTrue(type.accepts("IRON_ORE"));
        assertFalse(type.accepts("DIRT"));
    }

    @Test
    public void progressionLadderTiers() throws InvalidConfigurationException {
        YamlConfiguration config = yaml(String.join("\n",
                "progression:",
                "  tiers:",
                "    - id: novizio",
                "      sessions_required: 0",
                "      pay_multiplier: 1.0",
                "    - id: esperto",
                "      sessions_required: 150",
                "      pay_multiplier: 1.35",
                "  decay:",
                "    enabled: true",
                "    inactive_days_threshold: 45",
                "    decay_sessions_per_day: 0.5"));
        ProgressionLadder ladder = new ProgressionLadder();
        ladder.load(config.getConfigurationSection("progression"));
        assertEquals("novizio", ladder.tierFor(0).orElseThrow().id());
        assertEquals("novizio", ladder.tierFor(149).orElseThrow().id());
        assertEquals("esperto", ladder.tierFor(150).orElseThrow().id());
        assertEquals(1.35, ladder.multiplierFor("esperto"), 0.0001);
        assertTrue(ladder.decayEnabled());
        assertEquals(45, ladder.inactiveDaysThreshold());
    }

    @Test
    public void seasonalMultiplierFromMonth() {
        assertEquals(Season.WINTER, Season.fromMonth(1));
        assertEquals(Season.SPRING, Season.fromMonth(4));
        assertEquals(Season.SUMMER, Season.fromMonth(7));
        assertEquals(Season.AUTUMN, Season.fromMonth(10));
    }
}
