package dev.openrp.companies.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import dev.openrp.companies.model.Company;

/**
 * Fired before a company is registered and persisted. Cancel to abort creation. {@link #getCreator()}
 * is {@code null} when the company is created programmatically (API) or by staff on another player's
 * behalf.
 */
public class CompanyCreateEvent extends CompanyEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player creator;
    private boolean cancelled;

    public CompanyCreateEvent(Player creator, Company company) {
        super(company);
        this.creator = creator;
    }

    /** The creating player, or {@code null} if created via the API / admin flow. */
    public Player getCreator() {
        return creator;
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
