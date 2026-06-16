package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyLicenseStatus;
import dev.openrp.companies.model.CompanyLicenseType;

/** Fired after a company license has been granted, suspended or revoked. */
public class CompanyLicenseChangeEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyLicenseType licenseType;
    private final CompanyLicenseStatus previousStatus;
    private final CompanyLicenseStatus newStatus;

    public CompanyLicenseChangeEvent(Company company, CompanyLicenseType licenseType,
                                     CompanyLicenseStatus previousStatus, CompanyLicenseStatus newStatus) {
        super(company);
        this.licenseType = licenseType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public CompanyLicenseType getLicenseType() {
        return licenseType;
    }

    public CompanyLicenseStatus getPreviousStatus() {
        return previousStatus;
    }

    public CompanyLicenseStatus getNewStatus() {
        return newStatus;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
