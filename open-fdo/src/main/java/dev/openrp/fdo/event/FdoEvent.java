package dev.openrp.fdo.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Base for Open FDO events, so other plugins can observe state changes through the Bukkit event bus. */
public abstract class FdoEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
