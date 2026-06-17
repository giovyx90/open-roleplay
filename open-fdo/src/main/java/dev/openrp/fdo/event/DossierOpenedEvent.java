package dev.openrp.fdo.event;

import org.bukkit.event.HandlerList;
import dev.openrp.fdo.model.Dossier;

/** Fired after a dossier is opened. Observational; not cancellable. */
public final class DossierOpenedEvent extends FdoEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Dossier dossier;

    public DossierOpenedEvent(Dossier dossier) {
        this.dossier = dossier;
    }

    public Dossier dossier() {
        return dossier;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
