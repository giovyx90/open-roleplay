package dev.openrp.politics.adapter;

/**
 * Bridge to a company backend (e.g. Open Companies). Open Politics exposes which charges may revoke a
 * licence and recognises licence-concession acts; the actual licence lives in the company plugin. The
 * default is a no-op whose {@link #available()} reports {@code false}.
 */
public interface CompanyAdapter {

    String id();

    boolean available();

    /** Display name of a company id, or the id itself when the backend is absent. */
    String companyName(String companyId);

    /** Whether a company exists in the backend; always {@code false} on the no-op default. */
    boolean companyExists(String companyId);
}
