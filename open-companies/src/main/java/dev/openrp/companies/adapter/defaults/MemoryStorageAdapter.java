package dev.openrp.companies.adapter.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import dev.openrp.companies.adapter.StorageAdapter;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyTransaction;
import dev.openrp.companies.model.RecurringPayment;

/**
 * Non-persistent storage adapter that keeps everything in memory only. Handy for the
 * {@code adapters.storage: memory} demo mode and for unit-testing the services without touching disk.
 * Data is lost on restart.
 */
public final class MemoryStorageAdapter implements StorageAdapter {

    private final Map<String, Company> companies = new LinkedHashMap<>();
    private final Map<UUID, CompanyAsset> assets = new LinkedHashMap<>();
    private final Map<UUID, CompanyApplication> applications = new LinkedHashMap<>();
    private final List<CompanyTransaction> transactions = new ArrayList<>();
    private final Map<String, RecurringPayment> recurring = new LinkedHashMap<>();

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public void init() {
        // nothing to open
    }

    @Override
    public Collection<Company> loadCompanies() {
        return new ArrayList<>(companies.values());
    }

    @Override
    public void saveCompany(Company company) {
        companies.put(company.id(), company);
    }

    @Override
    public void deleteCompany(String companyId) {
        companies.remove(companyId);
    }

    @Override
    public Collection<CompanyAsset> loadAssets() {
        return new ArrayList<>(assets.values());
    }

    @Override
    public void saveAsset(CompanyAsset asset) {
        assets.put(asset.id(), asset);
    }

    @Override
    public void deleteAsset(UUID assetId) {
        assets.remove(assetId);
    }

    @Override
    public Collection<CompanyApplication> loadApplications() {
        return new ArrayList<>(applications.values());
    }

    @Override
    public void saveApplication(CompanyApplication application) {
        applications.put(application.id(), application);
    }

    @Override
    public void deleteApplication(UUID applicationId) {
        applications.remove(applicationId);
    }

    @Override
    public Collection<CompanyTransaction> loadTransactions() {
        return new ArrayList<>(transactions);
    }

    @Override
    public void appendTransaction(CompanyTransaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public void deleteTransactionsOf(String companyId) {
        transactions.removeIf(tx -> tx.companyId().equals(companyId));
    }

    @Override
    public Collection<RecurringPayment> loadRecurringPayments() {
        return new ArrayList<>(recurring.values());
    }

    @Override
    public void saveRecurringPayment(RecurringPayment payment) {
        recurring.put(payment.key(), payment);
    }

    @Override
    public void deleteRecurringPayment(String companyId, UUID memberUuid) {
        recurring.remove(RecurringPayment.key(companyId, memberUuid));
    }

    @Override
    public void flush() {
        // in memory, always durable for the lifetime of the process
    }

    @Override
    public void close() {
        // nothing to release
    }
}
