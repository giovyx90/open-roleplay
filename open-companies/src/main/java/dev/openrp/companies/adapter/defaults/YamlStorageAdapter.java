package dev.openrp.companies.adapter.defaults;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.companies.adapter.StorageAdapter;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyAssetType;
import dev.openrp.companies.model.CompanyLicenseStatus;
import dev.openrp.companies.model.CompanyLicenseType;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.companies.model.CompanyStatus;

/**
 * Default storage adapter persisting companies, assets and applications to a single YAML file. The
 * schema mirrors the model one-to-one, so the file doubles as human-readable documentation of what a
 * company "is". A relational adapter would implement the same interface with per-row writes - see the
 * README. Writes are infrequent (company lifecycle events), so each mutation rewrites the file; the
 * data set is small and this keeps the adapter simple and crash-safe.
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String COMPANIES = "companies";
    private static final String ASSETS = "assets";
    private static final String APPLICATIONS = "applications";

    private final File file;
    private final Logger logger;
    private YamlConfiguration yaml = new YamlConfiguration();

    public YamlStorageAdapter(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public String id() {
        return "yaml";
    }

    @Override
    public void init() {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    // --- companies ---------------------------------------------------------------------------

    @Override
    public Collection<Company> loadCompanies() {
        List<Company> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(COMPANIES);
        if (root == null) {
            return result;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                result.add(readCompany(id, section));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCompanies] Skipping malformed company '" + id + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveCompany(Company company) {
        writeCompany(company);
        persist();
    }

    @Override
    public void deleteCompany(String companyId) {
        yaml.set(COMPANIES + "." + companyId, null);
        persist();
    }

    // --- assets ------------------------------------------------------------------------------

    @Override
    public Collection<CompanyAsset> loadAssets() {
        List<CompanyAsset> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(ASSETS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(readAsset(UUID.fromString(key), section));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCompanies] Skipping malformed asset '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveAsset(CompanyAsset asset) {
        writeAsset(asset);
        persist();
    }

    @Override
    public void deleteAsset(UUID assetId) {
        yaml.set(ASSETS + "." + assetId, null);
        persist();
    }

    // --- applications ------------------------------------------------------------------------

    @Override
    public Collection<CompanyApplication> loadApplications() {
        List<CompanyApplication> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(APPLICATIONS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(readApplication(UUID.fromString(key), section));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCompanies] Skipping malformed application '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveApplication(CompanyApplication application) {
        writeApplication(application);
        persist();
    }

    @Override
    public void deleteApplication(UUID applicationId) {
        yaml.set(APPLICATIONS + "." + applicationId, null);
        persist();
    }

    // --- lifecycle ---------------------------------------------------------------------------

    @Override
    public void flush() {
        persist();
    }

    @Override
    public void close() {
        persist();
    }

    // --- serialization -----------------------------------------------------------------------

    private Company readCompany(String id, ConfigurationSection section) {
        String owner = section.getString("owner");
        Company company = new Company(
                id,
                section.getString("display-name", id),
                section.getString("type", "generic"),
                owner == null || owner.isBlank() ? null : UUID.fromString(owner),
                section.getLong("created-at"));
        company.setStatus(CompanyStatus.fromString(section.getString("status")));
        company.setBalance(section.getDouble("balance", 0.0));

        ConfigurationSection metadata = section.getConfigurationSection("metadata");
        if (metadata != null) {
            for (String key : metadata.getKeys(false)) {
                company.metadata().put(key, metadata.getString(key, ""));
            }
        }

        ConfigurationSection members = section.getConfigurationSection("members");
        if (members != null) {
            for (String uuid : members.getKeys(false)) {
                ConfigurationSection member = members.getConfigurationSection(uuid);
                if (member == null) {
                    continue;
                }
                CompanyRole role = CompanyRole.fromString(member.getString("role"));
                if (role == null) {
                    role = CompanyRole.EMPLOYEE;
                }
                company.addMember(new CompanyMember(
                        id,
                        UUID.fromString(uuid),
                        member.getString("name"),
                        role,
                        member.getDouble("salary", 0.0),
                        member.getLong("joined-at")));
            }
        }

        ConfigurationSection licenses = section.getConfigurationSection("licenses");
        if (licenses != null) {
            for (String key : licenses.getKeys(false)) {
                CompanyLicenseType type = CompanyLicenseType.fromString(key);
                if (type != null) {
                    company.setLicense(type, CompanyLicenseStatus.fromString(licenses.getString(key)));
                }
            }
        }

        ConfigurationSection hq = section.getConfigurationSection("headquarters");
        if (hq != null && hq.isString("world")) {
            company.setHeadquarters(new Company.Headquarters(
                    hq.getString("world", "world"),
                    hq.getDouble("x"),
                    hq.getDouble("y"),
                    hq.getDouble("z"),
                    (float) hq.getDouble("yaw", 0.0),
                    (float) hq.getDouble("pitch", 0.0)));
        }
        return company;
    }

    private void writeCompany(Company company) {
        String base = COMPANIES + "." + company.id();
        yaml.set(base, null);
        yaml.set(base + ".display-name", company.displayName());
        yaml.set(base + ".type", company.type());
        yaml.set(base + ".owner", company.ownerUuid() == null ? null : company.ownerUuid().toString());
        yaml.set(base + ".status", company.status().name());
        yaml.set(base + ".balance", company.balance());
        yaml.set(base + ".created-at", company.createdAt());

        for (Map.Entry<String, String> entry : company.metadata().entrySet()) {
            yaml.set(base + ".metadata." + entry.getKey(), entry.getValue());
        }
        for (CompanyMember member : company.members()) {
            String memberBase = base + ".members." + member.playerUuid();
            yaml.set(memberBase + ".name", member.playerName());
            yaml.set(memberBase + ".role", member.role().name());
            yaml.set(memberBase + ".salary", member.salary());
            yaml.set(memberBase + ".joined-at", member.joinedAt());
        }
        for (Map.Entry<CompanyLicenseType, CompanyLicenseStatus> entry : company.licenses().entrySet()) {
            yaml.set(base + ".licenses." + entry.getKey().key(), entry.getValue().name());
        }
        company.headquarters().ifPresent(hq -> {
            String hqBase = base + ".headquarters";
            yaml.set(hqBase + ".world", hq.world());
            yaml.set(hqBase + ".x", hq.x());
            yaml.set(hqBase + ".y", hq.y());
            yaml.set(hqBase + ".z", hq.z());
            yaml.set(hqBase + ".yaw", hq.yaw());
            yaml.set(hqBase + ".pitch", hq.pitch());
        });
    }

    private CompanyAsset readAsset(UUID id, ConfigurationSection section) {
        CompanyAssetType type = CompanyAssetType.fromString(section.getString("type"));
        if (type == null) {
            throw new IllegalArgumentException("unknown asset type '" + section.getString("type") + "'");
        }
        CompanyAsset asset = new CompanyAsset(
                id,
                section.getString("company", ""),
                type,
                new CompanyAsset.BlockPosition(
                        section.getString("world", "world"),
                        section.getInt("x"),
                        section.getInt("y"),
                        section.getInt("z")),
                section.getLong("created-at"));
        ConfigurationSection metadata = section.getConfigurationSection("metadata");
        if (metadata != null) {
            for (String key : metadata.getKeys(false)) {
                asset.metadata().put(key, metadata.getString(key, ""));
            }
        }
        return asset;
    }

    private void writeAsset(CompanyAsset asset) {
        String base = ASSETS + "." + asset.id();
        yaml.set(base, null);
        yaml.set(base + ".company", asset.companyId());
        yaml.set(base + ".type", asset.type().name());
        yaml.set(base + ".world", asset.position().world());
        yaml.set(base + ".x", asset.position().x());
        yaml.set(base + ".y", asset.position().y());
        yaml.set(base + ".z", asset.position().z());
        yaml.set(base + ".created-at", asset.createdAt());
        for (Map.Entry<String, String> entry : asset.metadata().entrySet()) {
            yaml.set(base + ".metadata." + entry.getKey(), entry.getValue());
        }
    }

    private CompanyApplication readApplication(UUID id, ConfigurationSection section) {
        CompanyApplication application = new CompanyApplication(
                id,
                UUID.fromString(section.getString("applicant", new UUID(0, 0).toString())),
                section.getString("applicant-name"),
                section.getString("requested-name", ""),
                section.getString("requested-type", "generic"),
                section.getString("description", ""),
                section.getLong("created-at"));
        try {
            application.setStatus(CompanyApplication.Status.valueOf(section.getString("status", "PENDING")));
        } catch (IllegalArgumentException ignored) {
            application.setStatus(CompanyApplication.Status.PENDING);
        }
        application.setResolution(section.getString("resolution"));
        return application;
    }

    private void writeApplication(CompanyApplication application) {
        String base = APPLICATIONS + "." + application.id();
        yaml.set(base, null);
        yaml.set(base + ".applicant", application.applicantUuid().toString());
        yaml.set(base + ".applicant-name", application.applicantName());
        yaml.set(base + ".requested-name", application.requestedName());
        yaml.set(base + ".requested-type", application.requestedType());
        yaml.set(base + ".description", application.description());
        yaml.set(base + ".created-at", application.createdAt());
        yaml.set(base + ".status", application.status().name());
        yaml.set(base + ".resolution", application.resolution());
    }

    private void persist() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            logger.severe("[OpenCompanies] Failed to save company data: " + exception.getMessage());
        }
    }
}
