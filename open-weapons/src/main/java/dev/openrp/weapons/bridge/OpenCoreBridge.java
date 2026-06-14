package dev.openrp.weapons.bridge;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public final class OpenCoreBridge {
    private final Object openCore;
    private final Logger logger;
    private boolean hudWarningLogged;

    private OpenCoreBridge(Object openCore, Logger logger) {
        this.openCore = openCore;
        this.logger = logger;
    }

    public static OpenCoreBridge connected(Object openCore, Logger logger) {
        return new OpenCoreBridge(openCore, logger);
    }

    public static OpenCoreBridge unavailable(Logger logger) {
        return new OpenCoreBridge(null, logger);
    }

    public boolean isPresent() {
        return openCore != null;
    }

    public void showHud(Player player, Component message, long ttlTicks) {
        if (openCore == null || player == null || message == null) {
            return;
        }
        try {
            Object hud = openCore.getClass().getMethod("hud").invoke(openCore);
            hud.getClass().getMethod("show", Player.class, Component.class, long.class)
                    .invoke(hud, player, message, ttlTicks);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | LinkageError error) {
            if (!hudWarningLogged && logger != null) {
                logger.warning("[OpenWeapons] Bridge HUD OpenCore non disponibile: " + rootMessage(error));
                hudWarningLogged = true;
            }
        }
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : error;
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
