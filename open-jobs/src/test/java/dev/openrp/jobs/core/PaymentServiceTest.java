package dev.openrp.jobs.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.jobs.config.Job;
import dev.openrp.jobs.model.PayoutBreakdown;
import dev.openrp.jobs.model.Season;
import dev.openrp.jobs.model.WorkSession;

public class PaymentServiceTest {

    private final PaymentService payment = new PaymentService();

    private Job job(String body) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(body);
        return Job.from("job", config.getConfigurationSection("job"));
    }

    private WorkSession session() {
        return new WorkSession("s", UUID.randomUUID(), "job", "loc", 1_000L);
    }

    private PaymentService.Context flat() {
        return new PaymentService.Context(1.0, 1, null, Season.SPRING, LocalTime.NOON, true, false);
    }

    @Test
    public void productionPaysPerUnitAboveMinimum() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: mine",
                "  payment_model: a_produzione",
                "  payment:",
                "    minimum_payout: 5.0",
                "    rates:",
                "      IRON_ORE: 5.0",
                "      STONE: 0.3"));
        WorkSession session = session();
        session.putProduced("IRON_ORE", 10);
        session.putProduced("STONE", 100);
        assertEquals(80.0, payment.basePay(job, session, 2_000L), 0.0001);
    }

    @Test
    public void productionBelowMinimumPaysNothing() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: mine",
                "  payment_model: a_produzione",
                "  payment:",
                "    minimum_payout: 50.0",
                "    rates:",
                "      STONE: 0.3"));
        WorkSession session = session();
        session.putProduced("STONE", 10); // 3.0 < 50.0
        assertEquals(0.0, payment.basePay(job, session, 2_000L), 0.0001);
    }

    @Test
    public void deliveryUsesDeliveryRates() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: forest",
                "  payment_model: a_consegna",
                "  payment:",
                "    delivery_location: segheria",
                "    delivery_rates:",
                "      OAK_LOG: 3.0"));
        WorkSession session = session();
        session.putProduced("OAK_LOG", 12);
        assertTrue(payment.isDelivery(job));
        assertEquals(36.0, payment.basePay(job, session, 2_000L), 0.0001);
    }

    @Test
    public void transformativePaysAccruedTransformationEarnings() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: workshop",
                "  payment_model: a_produzione",
                "  transformations:",
                "    - input:",
                "        - material: OAK_LOG",
                "          amount: 1",
                "      output:",
                "        - material: OAK_PLANKS",
                "          amount: 4",
                "      payout: 2.0"));
        WorkSession session = session();
        session.addTransformationEarnings(2.0);
        session.addTransformationEarnings(2.0);
        assertEquals(4.0, payment.basePay(job, session, 2_000L), 0.0001);
    }

    @Test
    public void sessionPayAppliesInactivityPenaltyBelowFloor() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: street",
                "  payment_model: a_sessione",
                "  payment:",
                "    rate_per_hour: 100.0",
                "    activity_threshold: 0.3",
                "    inactivity_penalty: 0.5"));
        WorkSession session = new WorkSession("s", UUID.randomUUID(), "job", "loc", 1_000L);
        session.bankActiveTime(1_000L + 3_600_000L); // exactly one active hour banked, no recorded activity minutes
        // active ratio is 0 (< 0.3) so the 0.5 penalty applies: 1h * 100 * 0.5 = 50.
        assertEquals(50.0, payment.basePay(job, session, 9_000_000L), 0.0001);
    }

    @Test
    public void multipliersStackOnTopOfBase() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: mine",
                "  payment_model: a_produzione",
                "  cooperative:",
                "    enabled: true",
                "    min_players: 2",
                "    max_bonus_players: 4",
                "    bonus_per_player: 0.05",
                "  payment:",
                "    rates:",
                "      IRON_ORE: 5.0"));
        WorkSession session = session();
        session.putProduced("IRON_ORE", 10); // base 50
        PaymentService.Context context = new PaymentService.Context(
                1.2, 3, null, Season.SPRING, LocalTime.NOON, true, false);
        PayoutBreakdown breakdown = payment.compute(job, session, 2_000L, context);
        assertEquals(50.0, breakdown.base(), 0.0001);
        assertEquals(1.2, breakdown.progressionMultiplier(), 0.0001);
        // 3 workers -> 1 + (3-1)*0.05 = 1.10
        assertEquals(1.10, breakdown.cooperativeMultiplier(), 0.0001);
        assertEquals(50.0 * 1.2 * 1.10, breakdown.total(), 0.0001);
    }

    @Test
    public void cooperativeDisabledGloballyIgnoresJobToggle() throws InvalidConfigurationException {
        Job job = job(String.join("\n",
                "job:",
                "  location_type: mine",
                "  payment_model: a_produzione",
                "  cooperative:",
                "    enabled: true",
                "    min_players: 2",
                "  payment:",
                "    rates:",
                "      IRON_ORE: 5.0"));
        WorkSession session = session();
        session.putProduced("IRON_ORE", 10);
        PaymentService.Context context = new PaymentService.Context(
                1.0, 4, null, Season.SPRING, LocalTime.NOON, false, false);
        assertEquals(1.0, payment.compute(job, session, 2_000L, context).cooperativeMultiplier(), 0.0001);
    }
}
