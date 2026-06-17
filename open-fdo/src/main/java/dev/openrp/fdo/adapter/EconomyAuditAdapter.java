package dev.openrp.fdo.adapter;

import java.util.List;
import java.util.UUID;

/**
 * Read-only economic audit, implementable on top of Open Companies or any economy plugin. Absent ->
 * the {@code ECONOMIC_AUDIT} capability is never effective and the matching act disappears from the
 * catalogue.
 */
public interface EconomyAuditAdapter {

    String id();

    /** Opens a read-only view of a subject's economic data for an auditor. */
    AuditView openReadOnlyAudit(UUID subject, UUID auditor);

    /** Snapshot to attach to a report. */
    AuditReport snapshot(UUID subject);

    /** A read-only set of labelled lines describing the subject's holdings/flows. */
    record AuditView(UUID subject, List<String> lines) {
    }

    /** A point-in-time snapshot summary attached to an audit act. */
    record AuditReport(UUID subject, String summary, long timestamp) {
    }
}
