package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyRole;

/** Fired after a member's role has changed. */
public class CompanyRoleChangeEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyMember member;
    private final CompanyRole previousRole;
    private final CompanyRole newRole;

    public CompanyRoleChangeEvent(Company company, CompanyMember member, CompanyRole previousRole, CompanyRole newRole) {
        super(company);
        this.member = member;
        this.previousRole = previousRole;
        this.newRole = newRole;
    }

    public CompanyMember getMember() {
        return member;
    }

    public CompanyRole getPreviousRole() {
        return previousRole;
    }

    public CompanyRole getNewRole() {
        return newRole;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
