package dev.openrp.companies.core;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.api.ChamberService;
import dev.openrp.companies.event.CompanyCreateEvent;
import dev.openrp.companies.event.CompanyHeadquartersChangeEvent;
import dev.openrp.companies.event.CompanyLicenseChangeEvent;
import dev.openrp.companies.event.CompanyStatusChangeEvent;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyLicenseStatus;
import dev.openrp.companies.model.CompanyLicenseType;
import dev.openrp.companies.model.CompanyStatus;

/**
 * Bukkit-facing implementation of {@link ChamberService}. Delegates rules and state to the pure
 * {@link CompanyManager} and adds events, region checks for headquarters and the audit log. Approval
 * routes the actual company creation through the (cancellable) create event so listeners see
 * application-born companies exactly like any other.
 */
public final class DefaultChamberService implements ChamberService {

    private final OpenCompaniesPlugin plugin;

    public DefaultChamberService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    private CompanyManager manager() {
        return plugin.companyManager();
    }

    // --- applications ------------------------------------------------------------------------

    @Override
    public CompanyResult submitApplication(UUID applicantUuid, String applicantName, String requestedName,
                                           String requestedType, String description) {
        CompanyResult result = manager().submitApplication(applicantUuid, applicantName, requestedName,
                requestedType, description);
        if (result.success()) {
            audit("APPLY", applicantName + " applied for '" + requestedName + "' (" + requestedType + ")");
        }
        return result;
    }

    @Override
    public CompanyResult approveApplication(UUID applicationId) {
        CompanyResult result = manager().approveApplication(applicationId);
        if (result.failed()) {
            return result;
        }
        Company company = result.company().orElse(null);
        if (company != null && fireCancelled(new CompanyCreateEvent(null, company))) {
            manager().delete(company.id());
            manager().reopenApplication(applicationId);
            return CompanyResult.fail("creation.cancelled");
        }
        audit("APPROVE", "Application " + applicationId + " approved"
                + (company == null ? "" : " -> '" + company.id() + "'"));
        return result;
    }

    @Override
    public CompanyResult denyApplication(UUID applicationId, String reason) {
        CompanyResult result = manager().denyApplication(applicationId, reason);
        if (result.success()) {
            audit("DENY", "Application " + applicationId + " denied: " + (reason == null ? "" : reason));
        }
        return result;
    }

    @Override
    public Collection<CompanyApplication> applications() {
        return manager().applications();
    }

    @Override
    public Collection<CompanyApplication> pendingApplications() {
        return manager().pendingApplications();
    }

    @Override
    public Optional<CompanyApplication> findApplication(UUID applicationId) {
        return manager().findApplication(applicationId);
    }

    @Override
    public Optional<CompanyApplication> findApplicationByShortId(String shortId) {
        return manager().findApplicationByShortId(shortId);
    }

    // --- licenses, status, headquarters ------------------------------------------------------

    @Override
    public boolean hasLicense(String companyId, CompanyLicenseType type) {
        return manager().company(companyId).map(company -> company.hasLicense(type)).orElse(false);
    }

    @Override
    public CompanyResult grantLicense(String companyId, CompanyLicenseType type) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyLicenseStatus previous = company.licenseStatus(type);
            CompanyResult result = manager().grantLicense(companyId, type);
            if (result.success()) {
                fire(new CompanyLicenseChangeEvent(company, type, previous, CompanyLicenseStatus.GRANTED));
                audit("LICENSE", "Granted " + (type == null ? "?" : type.key()) + " to '" + company.id() + "'");
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult revokeLicense(String companyId, CompanyLicenseType type) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyLicenseStatus previous = company.licenseStatus(type);
            CompanyResult result = manager().revokeLicense(companyId, type);
            if (result.success()) {
                fire(new CompanyLicenseChangeEvent(company, type, previous, CompanyLicenseStatus.REVOKED));
                audit("LICENSE", "Revoked " + (type == null ? "?" : type.key()) + " from '" + company.id() + "'");
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult setStatus(String companyId, CompanyStatus status) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            CompanyStatus previous = company.status();
            CompanyResult result = manager().setStatus(companyId, status);
            if (result.success()) {
                fire(new CompanyStatusChangeEvent(company, previous, status));
                audit("STATUS", "Company '" + company.id() + "' " + previous + " -> " + status);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult setHeadquarters(String companyId, OfflinePlayer actor, String world,
                                         double x, double y, double z, float yaw, float pitch) {
        Company company = manager().company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (actor != null && !plugin.adapters().region().canSetHeadquarters(actor, world, x, y, z)) {
            return CompanyResult.fail("hq.region_denied");
        }
        ReentrantLock lock = plugin.locks().get(company.id());
        lock.lock();
        try {
            Company.Headquarters previous = company.headquarters().orElse(null);
            Company.Headquarters headquarters = new Company.Headquarters(world, x, y, z, yaw, pitch);
            CompanyResult result = manager().setHeadquarters(companyId, headquarters);
            if (result.success()) {
                fire(new CompanyHeadquartersChangeEvent(company, previous, headquarters));
                audit("HQ", "Company '" + company.id() + "' HQ set in " + world);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    // --- helpers -----------------------------------------------------------------------------

    private void fire(Event event) {
        plugin.getServer().getPluginManager().callEvent(event);
    }

    private boolean fireCancelled(CompanyCreateEvent event) {
        fire(event);
        return event.isCancelled();
    }

    private void audit(String category, String message) {
        plugin.adapters().logging().log(category, message);
    }
}
