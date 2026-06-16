package dev.openrp.companies.adapter;

import java.util.Collection;
import java.util.UUID;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyAsset;

/**
 * Persistence backend for companies, assets and applications.
 *
 * <p>The interface is CRUD-shaped so a relational backend (SQLite/MySQL) can implement each
 * {@code save}/{@code delete} as a single-row upsert/delete, while the bundled YAML and in-memory
 * adapters can simply rewrite their structures. A company is stored as one aggregate (members,
 * licenses and headquarters included), mirroring how the model is shaped. The core calls the relevant
 * {@code save} after every mutating operation, so durability is the adapter's decision.</p>
 */
public interface StorageAdapter {

    String id();

    /** Open files/connections and create schema if needed. Called once on enable. */
    void init();

    // --- companies ---------------------------------------------------------------------------

    Collection<Company> loadCompanies();

    void saveCompany(Company company);

    void deleteCompany(String companyId);

    // --- assets ------------------------------------------------------------------------------

    Collection<CompanyAsset> loadAssets();

    void saveAsset(CompanyAsset asset);

    void deleteAsset(UUID assetId);

    // --- applications ------------------------------------------------------------------------

    Collection<CompanyApplication> loadApplications();

    void saveApplication(CompanyApplication application);

    void deleteApplication(UUID applicationId);

    // --- lifecycle ---------------------------------------------------------------------------

    /** Forces any buffered writes to durable storage. */
    void flush();

    /** Releases resources. Called on disable. */
    void close();
}
