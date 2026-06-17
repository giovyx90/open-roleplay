package dev.openrp.fdo.event;

import org.bukkit.event.HandlerList;
import dev.openrp.fdo.model.ActRecord;

/** Fired after an act is produced (signed, stamped and logged). Observational; not cancellable. */
public final class ActProducedEvent extends FdoEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ActRecord act;
    private final String dossierId;

    public ActProducedEvent(ActRecord act, String dossierId) {
        this.act = act;
        this.dossierId = dossierId;
    }

    public ActRecord act() {
        return act;
    }

    /** The dossier the act opened or touched, or {@code null}. */
    public String dossierId() {
        return dossierId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
