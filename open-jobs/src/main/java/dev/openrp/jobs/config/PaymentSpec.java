package dev.openrp.jobs.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The numbers behind a job's pay, read from its {@code payment:} block. Which fields matter depends on
 * the job's {@code payment_model}: {@code rates}/{@code minimum_payout} for production, the hourly
 * rate and activity floor for session pay, {@code delivery_rates}/{@code delivery_location} for
 * delivery pay. Materials are normalised to upper case so config casing never matters.
 */
public final class PaymentSpec {

    private final Map<String, Double> rates;
    private final Map<String, Double> deliveryRates;
    private final double minimumPayout;
    private final double ratePerHour;
    private final double activityThreshold;
    private final double inactivityPenalty;
    private final String deliveryLocation;

    public PaymentSpec(Map<String, Double> rates, Map<String, Double> deliveryRates, double minimumPayout,
                       double ratePerHour, double activityThreshold, double inactivityPenalty,
                       String deliveryLocation) {
        this.rates = rates;
        this.deliveryRates = deliveryRates;
        this.minimumPayout = Math.max(0.0, minimumPayout);
        this.ratePerHour = Math.max(0.0, ratePerHour);
        this.activityThreshold = clamp01(activityThreshold);
        this.inactivityPenalty = clamp01(inactivityPenalty);
        this.deliveryLocation = deliveryLocation;
    }

    public static PaymentSpec empty() {
        return new PaymentSpec(Map.of(), Map.of(), 0.0, 0.0, 0.3, 0.5, "");
    }

    public static PaymentSpec from(ConfigurationSection section) {
        if (section == null) {
            return empty();
        }
        return new PaymentSpec(
                rateMap(section.getConfigurationSection("rates")),
                rateMap(section.getConfigurationSection("delivery_rates")),
                section.getDouble("minimum_payout", 0.0),
                section.getDouble("rate_per_hour", 0.0),
                section.getDouble("activity_threshold", 0.3),
                section.getDouble("inactivity_penalty", 0.5),
                section.getString("delivery_location", ""));
    }

    private static Map<String, Double> rateMap(ConfigurationSection section) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                result.put(key.toUpperCase(Locale.ROOT), section.getDouble(key, 0.0));
            }
        }
        return result;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** Per-unit production rate for a material (upper-cased lookup), or 0 if it is not paid. */
    public double rate(String material) {
        return material == null ? 0.0 : rates.getOrDefault(material.toUpperCase(Locale.ROOT), 0.0);
    }

    /** Per-unit delivery rate for a material, or 0 if it is not paid on delivery. */
    public double deliveryRate(String material) {
        return material == null ? 0.0 : deliveryRates.getOrDefault(material.toUpperCase(Locale.ROOT), 0.0);
    }

    public Map<String, Double> rates() {
        return rates;
    }

    public Map<String, Double> deliveryRates() {
        return deliveryRates;
    }

    public boolean isPaidMaterial(String material) {
        if (material == null) {
            return false;
        }
        String key = material.toUpperCase(Locale.ROOT);
        return rates.containsKey(key) || deliveryRates.containsKey(key);
    }

    public double minimumPayout() {
        return minimumPayout;
    }

    public double ratePerHour() {
        return ratePerHour;
    }

    public double activityThreshold() {
        return activityThreshold;
    }

    public double inactivityPenalty() {
        return inactivityPenalty;
    }

    public String deliveryLocation() {
        return deliveryLocation;
    }
}
