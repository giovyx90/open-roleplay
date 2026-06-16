package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;

/** Fired after a company's headquarters location has been set or moved. */
public class CompanyHeadquartersChangeEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Company.Headquarters previousHeadquarters;
    private final Company.Headquarters newHeadquarters;

    public CompanyHeadquartersChangeEvent(Company company, Company.Headquarters previousHeadquarters,
                                          Company.Headquarters newHeadquarters) {
        super(company);
        this.previousHeadquarters = previousHeadquarters;
        this.newHeadquarters = newHeadquarters;
    }

    /** Previous headquarters, or {@code null} if the company had none. */
    public Company.Headquarters getPreviousHeadquarters() {
        return previousHeadquarters;
    }

    public Company.Headquarters getNewHeadquarters() {
        return newHeadquarters;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
