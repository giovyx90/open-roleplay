package dev.openrp.jobs.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.jobs.model.PaymentModel;

/**
 * A job definition, built entirely from config - the core knows no job by name. A job is a physical
 * activity anyone can do without institutional mediation: go, work, get paid. It binds a location type,
 * an activity to track, a payment model and the optional layers (licence, progression, cooperative,
 * tools, shifts, season, transformations) that a server may switch on.
 */
public final class Job {

    private final String id;
    private final String displayName;
    private final String category;
    private final String locationType;
    private final PaymentModel paymentModel;
    private final boolean requiresLicense;
    private final boolean progressionEnabled;
    private final LicenseSpec license;
    private final CooperativeSpec cooperative;
    private final ToolSpec tool;
    private final PaymentSpec payment;
    private final SeasonalSpec seasonal;
    private final ShiftSpec shift;
    private final List<Transformation> transformations;

    public Job(String id, String displayName, String category, String locationType, PaymentModel paymentModel,
               boolean requiresLicense, boolean progressionEnabled, LicenseSpec license,
               CooperativeSpec cooperative, ToolSpec tool, PaymentSpec payment, SeasonalSpec seasonal,
               ShiftSpec shift, List<Transformation> transformations) {
        this.id = id;
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        this.category = category;
        this.locationType = locationType;
        this.paymentModel = paymentModel;
        this.requiresLicense = requiresLicense;
        this.progressionEnabled = progressionEnabled;
        this.license = license;
        this.cooperative = cooperative;
        this.tool = tool;
        this.payment = payment;
        this.seasonal = seasonal;
        this.shift = shift;
        this.transformations = transformations;
    }

    public static Job from(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        List<Transformation> transformations = new ArrayList<>();
        for (java.util.Map<?, ?> raw : section.getMapList("transformations")) {
            Transformation transformation = Transformation.from(raw);
            if (transformation != null) {
                transformations.add(transformation);
            }
        }
        return new Job(
                id,
                section.getString("display_name", id),
                section.getString("category", "estrattivo"),
                section.getString("location_type", ""),
                PaymentModel.fromString(section.getString("payment_model")),
                section.getBoolean("requires_license", false),
                section.getBoolean("progression_enabled", true),
                LicenseSpec.from(section.getConfigurationSection("license")),
                CooperativeSpec.from(section.getConfigurationSection("cooperative")),
                ToolSpec.from(section.getConfigurationSection("required_tool")),
                PaymentSpec.from(section.getConfigurationSection("payment")),
                SeasonalSpec.from(section.getConfigurationSection("seasonal")),
                ShiftSpec.from(section.getConfigurationSection("shifts")),
                Collections.unmodifiableList(transformations));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String category() {
        return category;
    }

    public String locationType() {
        return locationType;
    }

    public PaymentModel paymentModel() {
        return paymentModel;
    }

    public boolean requiresLicense() {
        return requiresLicense;
    }

    public boolean progressionEnabled() {
        return progressionEnabled;
    }

    public LicenseSpec license() {
        return license;
    }

    public CooperativeSpec cooperative() {
        return cooperative;
    }

    public ToolSpec tool() {
        return tool;
    }

    public PaymentSpec payment() {
        return payment;
    }

    public SeasonalSpec seasonal() {
        return seasonal;
    }

    public ShiftSpec shift() {
        return shift;
    }

    public List<Transformation> transformations() {
        return transformations;
    }

    public boolean isTransformative() {
        return !transformations.isEmpty();
    }
}
