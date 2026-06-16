package dev.openrp.companies.event;

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyRole;

/**
 * Fired before an invitation is created. Cancel to block the invite. Carries the target player's id
 * and the role they are being invited to.
 */
public class CompanyMemberInviteEvent extends CompanyEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID inviterUuid;
    private final UUID targetUuid;
    private final CompanyRole role;
    private boolean cancelled;

    public CompanyMemberInviteEvent(Company company, UUID inviterUuid, UUID targetUuid, CompanyRole role) {
        super(company);
        this.inviterUuid = inviterUuid;
        this.targetUuid = targetUuid;
        this.role = role;
    }

    /** Id of the player who sent the invite, or {@code null} for API/admin invites. */
    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public CompanyRole getRole() {
        return role;
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
