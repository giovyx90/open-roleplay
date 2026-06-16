package dev.openrp.companies.adapter.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;

public class YamlStorageAdapterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Logger LOGGER = Logger.getLogger(YamlStorageAdapterTest.class.getName());

    private YamlStorageAdapter open(File file) {
        YamlStorageAdapter adapter = new YamlStorageAdapter(file, LOGGER);
        adapter.init();
        return adapter;
    }

    private Company sampleCompany() {
        UUID owner = UUID.randomUUID();
        Company company = new Company("acme", "Acme", "generic", owner, 123L);
        company.addMember(new CompanyMember("acme", owner, "Owner", CompanyRole.CEO, 0.0, 123L));
        return company;
    }

    @Test
    public void companyRoundTripsAcrossReopen() throws Exception {
        File file = new File(folder.getRoot(), "companies-data.yml");
        open(file).saveCompany(sampleCompany());

        YamlStorageAdapter reopened = open(file);
        assertEquals(1, reopened.loadCompanies().size());
        assertEquals("acme", reopened.loadCompanies().iterator().next().id());
    }

    @Test
    public void writeIsAtomicAndKeepsBackup() {
        File file = new File(folder.getRoot(), "companies-data.yml");
        YamlStorageAdapter adapter = open(file);
        adapter.saveCompany(sampleCompany());

        // Live file committed, no leftover temp file, backup created on the second write.
        assertTrue(file.isFile());
        assertFalse(new File(folder.getRoot(), "companies-data.yml.tmp").exists());

        Company another = new Company("globex", "Globex", "generic", UUID.randomUUID(), 1L);
        adapter.saveCompany(another);
        assertTrue(new File(folder.getRoot(), "companies-data.yml.bak").isFile());
    }

    @Test
    public void recoversFromCorruptPrimaryUsingBackup() throws Exception {
        File file = new File(folder.getRoot(), "companies-data.yml");
        YamlStorageAdapter adapter = open(file);
        adapter.saveCompany(sampleCompany());
        // Second write rotates the good first copy into the backup.
        adapter.saveCompany(new Company("globex", "Globex", "generic", UUID.randomUUID(), 1L));

        // Simulate a crash that left the live file truncated/garbage.
        Files.write(file.toPath(), "{{{ not: valid: yaml".getBytes(StandardCharsets.UTF_8));

        YamlStorageAdapter recovered = open(file);
        // Backup held the first (acme-only) snapshot; recovery must not silently return empty.
        assertFalse(recovered.loadCompanies().isEmpty());
        assertTrue(recovered.loadCompanies().stream().anyMatch(c -> c.id().equals("acme")));
    }

    @Test
    public void skipsCorruptMemberInsteadOfWholeCompany() throws Exception {
        File file = new File(folder.getRoot(), "companies-data.yml");
        open(file).saveCompany(sampleCompany());

        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        // Inject a member whose key is not a valid UUID.
        content = content.replace("    members:",
                "    members:\n      not-a-uuid:\n        name: Bad\n        role: EMPLOYEE\n        salary: 0.0\n        joined-at: 0");
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));

        YamlStorageAdapter reopened = open(file);
        // Company survives; only the bad member row is dropped.
        assertEquals(1, reopened.loadCompanies().size());
        assertEquals(1, reopened.loadCompanies().iterator().next().members().size());
    }
}
