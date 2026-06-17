package dev.openrp.fdo.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable holder for the active adapter set, exposed through the public API so any plugin can swap or
 * register an adapter at runtime (typically in its own {@code onEnable}). The five core adapters
 * always have a working default and reject {@code null}; the optional, world-facing adapters
 * (detention, economy audit, external records, radio, evidence source) start absent and a capability
 * tied to a missing adapter is silently disabled.
 */
public final class AdapterRegistry {

    private StorageAdapter storage;
    private PermissionAdapter permission;
    private NotificationAdapter notification;
    private LoggingAdapter logging;
    private RegionAdapter region;
    private DutyStatusAdapter duty;

    private DetentionAdapter detention;
    private EconomyAuditAdapter economyAudit;
    private RadioAdapter radio;
    private EvidenceSourceAdapter evidenceSource;
    private final List<ExternalRecordAdapter> externalRecords = new ArrayList<>();

    // --- core adapters (always present) ------------------------------------------------------

    public StorageAdapter storage() {
        return storage;
    }

    public void setStorage(StorageAdapter storage) {
        this.storage = Objects.requireNonNull(storage, "storage adapter");
    }

    public PermissionAdapter permission() {
        return permission;
    }

    public void setPermission(PermissionAdapter permission) {
        this.permission = Objects.requireNonNull(permission, "permission adapter");
    }

    public NotificationAdapter notification() {
        return notification;
    }

    public void setNotification(NotificationAdapter notification) {
        this.notification = Objects.requireNonNull(notification, "notification adapter");
    }

    public LoggingAdapter logging() {
        return logging;
    }

    public void setLogging(LoggingAdapter logging) {
        this.logging = Objects.requireNonNull(logging, "logging adapter");
    }

    public RegionAdapter region() {
        return region;
    }

    public void setRegion(RegionAdapter region) {
        this.region = Objects.requireNonNull(region, "region adapter");
    }

    public DutyStatusAdapter duty() {
        return duty;
    }

    public void setDuty(DutyStatusAdapter duty) {
        this.duty = Objects.requireNonNull(duty, "duty adapter");
    }

    // --- optional adapters -------------------------------------------------------------------

    public Optional<DetentionAdapter> detention() {
        return Optional.ofNullable(detention);
    }

    /** Registers (or clears, with {@code null}) the detention adapter. */
    public void setDetention(DetentionAdapter detention) {
        this.detention = detention;
    }

    public Optional<EconomyAuditAdapter> economyAudit() {
        return Optional.ofNullable(economyAudit);
    }

    public void setEconomyAudit(EconomyAuditAdapter economyAudit) {
        this.economyAudit = economyAudit;
    }

    public Optional<RadioAdapter> radio() {
        return Optional.ofNullable(radio);
    }

    public void setRadio(RadioAdapter radio) {
        this.radio = radio;
    }

    public Optional<EvidenceSourceAdapter> evidenceSource() {
        return Optional.ofNullable(evidenceSource);
    }

    public void setEvidenceSource(EvidenceSourceAdapter evidenceSource) {
        this.evidenceSource = evidenceSource;
    }

    public List<ExternalRecordAdapter> externalRecords() {
        return List.copyOf(externalRecords);
    }

    public void addExternalRecord(ExternalRecordAdapter adapter) {
        if (adapter != null) {
            externalRecords.add(adapter);
        }
    }

    public Optional<ExternalRecordAdapter> externalRecord(String recordType) {
        return externalRecords.stream()
                .filter(adapter -> adapter.recordType().equalsIgnoreCase(recordType))
                .findFirst();
    }

    // --- act/capability gating ---------------------------------------------------------------

    /**
     * Whether the named optional adapter is available. Used to decide if an act whose
     * {@code requires_adapter} (or whose capability) names this id should be offered at all. A blank
     * id means "no requirement" -> available. An unrecognised, non-blank id is treated as an
     * unsatisfiable requirement (fail-closed) so a config typo in {@code requires_adapter} hides the
     * act rather than silently offering an act whose effect can never run.
     */
    public boolean hasAdapter(String adapterId) {
        if (adapterId == null || adapterId.isBlank()) {
            return true;
        }
        return switch (adapterId.toUpperCase(Locale.ROOT)) {
            case "DETENTION" -> detention != null;
            case "ECONOMY_AUDIT" -> economyAudit != null;
            case "EXTERNAL_RECORD" -> !externalRecords.isEmpty();
            case "RADIO" -> radio != null;
            case "EVIDENCE_SOURCE" -> evidenceSource != null;
            default -> false;
        };
    }
}
