package dev.openrp.companies.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;

/**
 * Fired before a company is deleted and its data removed. Cancel to abort. Listeners that own
 * company-linked data (assets, documents, ...) should use this to clean up.
 */
public class CompanyDeleteEvent extends CompanyEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    public CompanyDeleteEvent(Company company) {
        super(company);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
