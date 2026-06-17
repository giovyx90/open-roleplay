package dev.openrp.fdo.capability;

import java.util.Locale;
import java.util.Optional;

/**
 * Abstract permission to perform a <em>class</em> of acts. This is the central concept of the
 * adapter-first design: acts are not bound to authorities, they are bound to capabilities, and
 * capabilities are assigned to ranks entirely from configuration. The core defines <em>what</em>
 * each capability does; the config decides <em>who</em> holds it.
 *
 * <p>Each capability optionally declares the adapter it requires. When an act needs a capability
 * whose adapter is absent at runtime, the act is silently unavailable - no crash, no hard
 * dependency. A setting with no economy simply never assigns {@link #ECONOMIC_AUDIT} to any rank
 * and the matching act ceases to exist on that server.</p>
 */
public enum Capability {

    /** Register a timed temporary detainment. */
    DETAIN_TEMPORARY(null),
    /** Open a dossier with cautionary custody. */
    ARREST(null),
    /** Add a charge to a dossier. */
    ADD_CHARGE(null),
    /** Seize and mark an evidence item. */
    SEIZE_EVIDENCE(null),
    /** Register a monetary sanction. */
    ISSUE_FINE(null),
    /** Add a subject to the wanted register. */
    FLAG_WANTED(null),
    /** Open an investigative dossier. */
    OPEN_INVESTIGATION(null),
    /** Request a warrant from the judging authority. */
    REQUEST_WARRANT(null),
    /** Issue a warrant (judging authority). */
    ISSUE_WARRANT(null),
    /** Issue a verdict. */
    ISSUE_VERDICT(null),
    /** Extend custody. */
    EXTEND_CUSTODY(null),
    /** Economic audit (requires an economy-audit adapter). */
    ECONOMIC_AUDIT("ECONOMY_AUDIT"),
    /** Attach external records/acts (requires an external-record adapter). */
    IMPORT_EXTERNAL_RECORD("EXTERNAL_RECORD"),
    /** Manage detention (requires a detention adapter). */
    MANAGE_DETENTION("DETENTION"),
    /** Declare an alert state. */
    DECLARE_ALERT(null);

    private final String requiredAdapter;

    Capability(String requiredAdapter) {
        this.requiredAdapter = requiredAdapter;
    }

    /**
     * The adapter id this capability needs, or empty when the capability is fully self-contained in
     * the core. {@link dev.openrp.fdo.config.ActDefinition Acts} may declare their own adapter
     * requirement on top of this one.
     */
    public Optional<String> requiredAdapter() {
        return Optional.ofNullable(requiredAdapter);
    }

    /** Case-insensitive lookup; returns empty for unknown ids so config typos are skipped, not fatal. */
    public static Optional<Capability> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Capability.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
