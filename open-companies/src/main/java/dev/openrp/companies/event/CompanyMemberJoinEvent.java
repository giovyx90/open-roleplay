package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;

/** Fired after a player has joined a company (accepted an invite or was added by an admin). */
public class CompanyMemberJoinEvent extends CompanyEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyMember member;

    public CompanyMemberJoinEvent(Company company, CompanyMember member) {
        super(company);
        this.member = member;
    }

    public CompanyMember getMember() {
        return member;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
