package dev.openrp.companies.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A pending request to found a company, used by the {@code PLAYER_APPLICATION} creation mode. Players
 * submit one; staff later approve (which creates the company with the applicant as CEO) or deny it.
 * Persisted so applications survive restarts.
 */
public final class CompanyApplication {

    /** Lifecycle of an application. */
    public enum Status {
        PENDING,
        APPROVED,
        DENIED
    }

    private final UUID id;
    private final UUID applicantUuid;
    private String applicantName;
    private final String requestedName;
    private final String requestedType;
    private final String description;
    private final long createdAt;
    private Status status;
    private String resolution;

    public CompanyApplication(UUID id, UUID applicantUuid, String applicantName, String requestedName,
                              String requestedType, String description, long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.applicantUuid = Objects.requireNonNull(applicantUuid, "applicantUuid");
        this.applicantName = applicantName;
        this.requestedName = Objects.requireNonNull(requestedName, "requestedName");
        this.requestedType = requestedType == null ? "generic" : requestedType;
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
        this.status = Status.PENDING;
    }

    public UUID id() {
        return id;
    }

    /** First 8 characters of the id; what admins type in {@code /company admin approve <id>}. */
    public String shortId() {
        return id.toString().substring(0, 8);
    }

    public UUID applicantUuid() {
        return applicantUuid;
    }

    public String applicantName() {
        return applicantName == null ? applicantUuid.toString() : applicantName;
    }

    public void setApplicantName(String applicantName) {
        if (applicantName != null && !applicantName.isBlank()) {
            this.applicantName = applicantName;
        }
    }

    public String requestedName() {
        return requestedName;
    }

    public String requestedType() {
        return requestedType;
    }

    public String description() {
        return description;
    }

    public long createdAt() {
        return createdAt;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public String resolution() {
        return resolution == null ? "" : resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
