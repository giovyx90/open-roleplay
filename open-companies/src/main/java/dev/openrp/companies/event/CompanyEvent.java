package dev.openrp.companies.event;

import org.bukkit.event.Event;
import dev.openrp.companies.model.Company;

/**
 * Base class for every Open Companies event. Carries the affected company; each concrete subclass
 * keeps its own {@code HandlerList} as Bukkit requires. All Open Companies events are fired on the
 * main server thread.
 */
public abstract class CompanyEvent extends Event {

    private final Company company;

    protected CompanyEvent(Company company) {
        this.company = company;
    }

    public Company getCompany() {
        return company;
    }
}
