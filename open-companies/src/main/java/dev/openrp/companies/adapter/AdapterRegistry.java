package dev.openrp.companies.adapter;

import java.util.Objects;

/**
 * Mutable holder for the active adapter set. Exposed through the public API so any plugin can swap
 * one or more adapters at runtime (typically in its own {@code onEnable}, after Open Companies has
 * loaded) without touching the core. Setters reject {@code null} to guarantee the core always has a
 * working implementation behind every interface.
 */
public final class AdapterRegistry {

    private StorageAdapter storage;
    private EconomyAdapter economy;
    private PermissionAdapter permission;
    private RegionAdapter region;
    private IdentityAdapter identity;
    private NotificationAdapter notification;
    private LoggingAdapter logging;

    public StorageAdapter storage() {
        return storage;
    }

    public void setStorage(StorageAdapter storage) {
        this.storage = Objects.requireNonNull(storage, "storage adapter");
    }

    public EconomyAdapter economy() {
        return economy;
    }

    public void setEconomy(EconomyAdapter economy) {
        this.economy = Objects.requireNonNull(economy, "economy adapter");
    }

    public PermissionAdapter permission() {
        return permission;
    }

    public void setPermission(PermissionAdapter permission) {
        this.permission = Objects.requireNonNull(permission, "permission adapter");
    }

    public RegionAdapter region() {
        return region;
    }

    public void setRegion(RegionAdapter region) {
        this.region = Objects.requireNonNull(region, "region adapter");
    }

    public IdentityAdapter identity() {
        return identity;
    }

    public void setIdentity(IdentityAdapter identity) {
        this.identity = Objects.requireNonNull(identity, "identity adapter");
    }

    public NotificationAdapter notification() {
        return notification;
    }

    public void setNotification(NotificationAdapter notification) {
        this.notification = Objects.requireNonNull(notification, "notification adapter");
    }

    public LoggingAdapter logging() {
        return logging;
    }

    public void setLogging(LoggingAdapter logging) {
        this.logging = Objects.requireNonNull(logging, "logging adapter");
    }
}
