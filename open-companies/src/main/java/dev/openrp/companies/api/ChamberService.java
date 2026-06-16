package dev.openrp.companies.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyLicenseType;
import dev.openrp.companies.model.CompanyStatus;

/**
 * The "chamber of commerce": the administrative side of companies. Handles the application queue
 * (when {@code creation.mode} is {@code PLAYER_APPLICATION}), legal status, licenses and the
 * headquarters. Vertical modules gate their gameplay on {@link #hasLicense(String, CompanyLicenseType)}.
 */
public interface ChamberService {

    // --- applications ------------------------------------------------------------------------

    CompanyResult submitApplication(UUID applicantUuid, String applicantName, String requestedName,
                                    String requestedType, String description);

    /** Approves a pending application, creating the company with the applicant as CEO. */
    CompanyResult approveApplication(UUID applicationId);

    CompanyResult denyApplication(UUID applicationId, String reason);

    Collection<CompanyApplication> applications();

    Collection<CompanyApplication> pendingApplications();

    Optional<CompanyApplication> findApplication(UUID applicationId);

    /** Convenience lookup by the 8-char short id shown in {@code /company admin applications}. */
    Optional<CompanyApplication> findApplicationByShortId(String shortId);

    // --- licenses, status, headquarters ------------------------------------------------------

    boolean hasLicense(String companyId, CompanyLicenseType type);

    CompanyResult grantLicense(String companyId, CompanyLicenseType type);

    CompanyResult revokeLicense(String companyId, CompanyLicenseType type);

    CompanyResult setStatus(String companyId, CompanyStatus status);

    /** Sets the headquarters at the given coordinates, consulting the region adapter for {@code actor}. */
    CompanyResult setHeadquarters(String companyId, OfflinePlayer actor, String world,
                                  double x, double y, double z, float yaw, float pitch);
}
