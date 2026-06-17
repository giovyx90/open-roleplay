package dev.openrp.jobs.adapter;

import java.util.Objects;

/**
 * Mutable holder for the active adapter set, exposed through the public API so any plugin can swap an
 * adapter at runtime (typically in its own {@code onEnable}). Every adapter always has a working
 * default and rejects {@code null}: the world-facing ones (economy, company, identity) start as a
 * bundled default whose {@code available()} reports {@code false}, so a feature that needs a real
 * backend degrades cleanly instead of crashing.
 */
public final class AdapterRegistry {

    private StorageAdapter storage;
    private PermissionAdapter permission;
    private NotificationAdapter notification;
    private RegionAdapter region;
    private EconomyAdapter economy;
    private CompanyEmploymentAdapter company;
    private IdentityAdapter identity;

    public StorageAdapter storage() {
        return storage;
    }

    public void setStorage(StorageAdapter storage) {
        this.storage = Objects.requireNonNull(storage, "storage adapter");
    }

    public PermissionAdapter permission() {
        return permission;
    }

    public void setPermission(PermissionAdapter permission) {
        this.permission = Objects.requireNonNull(permission, "permission adapter");
    }

    public NotificationAdapter notification() {
        return notification;
    }

    public void setNotification(NotificationAdapter notification) {
        this.notification = Objects.requireNonNull(notification, "notification adapter");
    }

    public RegionAdapter region() {
        return region;
    }

    public void setRegion(RegionAdapter region) {
        this.region = Objects.requireNonNull(region, "region adapter");
    }

    public EconomyAdapter economy() {
        return economy;
    }

    public void setEconomy(EconomyAdapter economy) {
        this.economy = Objects.requireNonNull(economy, "economy adapter");
    }

    public CompanyEmploymentAdapter company() {
        return company;
    }

    public void setCompany(CompanyEmploymentAdapter company) {
        this.company = Objects.requireNonNull(company, "company adapter");
    }

    public IdentityAdapter identity() {
        return identity;
    }

    public void setIdentity(IdentityAdapter identity) {
        this.identity = Objects.requireNonNull(identity, "identity adapter");
    }
}
