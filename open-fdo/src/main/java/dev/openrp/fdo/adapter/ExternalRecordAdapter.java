package dev.openrp.fdo.adapter;

import java.util.Optional;

/**
 * Attaches an external document (a medical referral, a notarial deed, ...) to a dossier by id plus a
 * verification hash. Generalises the v2 "medical record" into one contract; several implementations
 * may coexist, distinguished by {@link #recordType()}. Absent -> the {@code IMPORT_EXTERNAL_RECORD}
 * capability is inert.
 */
public interface ExternalRecordAdapter {

    /** Discriminator such as {@code "medical"} or {@code "notarial"}. */
    String recordType();

    Optional<ExternalRecord> fetch(String recordId);

    boolean verifyIntegrity(String recordId, String hash);

    /** A fetched external document reference. */
    record ExternalRecord(String recordType, String recordId, String summary, String issuedBy, long timestamp) {
    }
}
