package dev.openrp.companies.event;

import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;

/** Fired after a player has left a company, whether voluntarily, fired or because it was dissolved. */
public class CompanyMemberLeaveEvent extends CompanyEvent {

    /** Why the member left. */
    public enum Reason {
        LEFT,
        FIRED,
        DISSOLVED
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final CompanyMember member;
    private final Reason reason;

    public CompanyMemberLeaveEvent(Company company, CompanyMember member, Reason reason) {
        super(company);
        this.member = member;
        this.reason = reason;
    }

    public CompanyMember getMember() {
        return member;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
