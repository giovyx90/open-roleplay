package dev.openrp.vending.adapter;

import java.util.Objects;

/**
 * Mutable holder for the active adapter set. Exposed through the public API so any plugin can swap
 * one or more adapters at runtime (typically in its own {@code onEnable}, after Open Vending
 * Machines has loaded) without touching the core. Setters reject {@code null} to guarantee the core
 * always has a working implementation behind every interface.
 */
public final class AdapterRegistry {

    private EconomyAdapter economy;
    private InventoryAdapter inventory;
    private BusinessAdapter business;
    private PermissionAdapter permission;
    private StorageAdapter storage;
    private NotificationAdapter notification;
    private LoggingAdapter logging;

    public EconomyAdapter economy() {
        return economy;
    }

    public void setEconomy(EconomyAdapter economy) {
        this.economy = Objects.requireNonNull(economy, "economy adapter");
    }

    public InventoryAdapter inventory() {
        return inventory;
    }

    public void setInventory(InventoryAdapter inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory adapter");
    }

    public BusinessAdapter business() {
        return business;
    }

    public void setBusiness(BusinessAdapter business) {
        this.business = Objects.requireNonNull(business, "business adapter");
    }

    public PermissionAdapter permission() {
        return permission;
    }

    public void setPermission(PermissionAdapter permission) {
        this.permission = Objects.requireNonNull(permission, "permission adapter");
    }

    public StorageAdapter storage() {
        return storage;
    }

    public void setStorage(StorageAdapter storage) {
        this.storage = Objects.requireNonNull(storage, "storage adapter");
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
