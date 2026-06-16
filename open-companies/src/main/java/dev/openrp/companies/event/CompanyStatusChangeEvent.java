package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyStatus;

/** Fired after a company's legal/operational status has changed. */
public class CompanyStatusChangeEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyStatus previousStatus;
    private final CompanyStatus newStatus;

    public CompanyStatusChangeEvent(Company company, CompanyStatus previousStatus, CompanyStatus newStatus) {
        super(company);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public CompanyStatus getPreviousStatus() {
        return previousStatus;
    }

    public CompanyStatus getNewStatus() {
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
