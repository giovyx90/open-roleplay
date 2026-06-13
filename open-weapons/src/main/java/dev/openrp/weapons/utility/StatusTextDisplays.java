package dev.openrp.weapons.utility;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public final class StatusTextDisplays {
    private StatusTextDisplays() {
    }

    public static TextDisplay spawn(Player target, Component text, double yOffset) {
        Location location = statusLocation(target, yOffset);
        return target.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setDefaultBackground(false);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setPersistent(false);
        });
    }

    public static void follow(TextDisplay display, Player target, double yOffset) {
        if (display != null && display.isValid() && target != null && target.isOnline() && !target.isDead()) {
            display.teleport(statusLocation(target, yOffset));
        }
    }

    private static Location statusLocation(Player target, double yOffset) {
        return target.getLocation().add(0.0D, yOffset, 0.0D);
    }
}
