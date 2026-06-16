package dev.openrp.companies.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Test;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyAssetType;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;

public class MemoryStorageAdapterTest {

    @Test
    public void companyRoundTripsAndDeletes() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        UUID owner = UUID.randomUUID();
        Company company = new Company("acme", "Acme", "generic", owner, 123L);
        company.addMember(new CompanyMember("acme", owner, "Owner", CompanyRole.CEO, 0.0, 123L));
        storage.saveCompany(company);

        assertEquals(1, storage.loadCompanies().size());
        assertEquals("acme", storage.loadCompanies().iterator().next().id());

        storage.deleteCompany("acme");
        assertTrue(storage.loadCompanies().isEmpty());
    }

    @Test
    public void assetRoundTrips() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        UUID id = UUID.randomUUID();
        storage.saveAsset(new CompanyAsset(id, "acme", CompanyAssetType.POS,
                new CompanyAsset.BlockPosition("world", 1, 2, 3), 0L));

        assertEquals(1, storage.loadAssets().size());
        storage.deleteAsset(id);
        assertTrue(storage.loadAssets().isEmpty());
    }

    @Test
    public void applicationRoundTrips() {
        MemoryStorageAdapter storage = new MemoryStorageAdapter();
        UUID id = UUID.randomUUID();
        storage.saveApplication(new CompanyApplication(id, UUID.randomUUID(), "Applicant",
                "New Co", "generic", "please", 0L));

        assertEquals(1, storage.loadApplications().size());
        storage.deleteApplication(id);
        assertTrue(storage.loadApplications().isEmpty());
    }
}
