package dev.openrp.access.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class AccessMessages {
    private AccessMessages() {
    }

    public static Component info(String title, String message) {
        return prefixed(title, NamedTextColor.AQUA, message, NamedTextColor.GRAY);
    }

    public static Component success(String title, String message) {
        return prefixed(title, NamedTextColor.GREEN, message, NamedTextColor.GRAY);
    }

    public static Component warning(String title, String message) {
        return prefixed(title, NamedTextColor.YELLOW, message, NamedTextColor.GRAY);
    }

    public static Component error(String title, String message) {
        return prefixed(title, NamedTextColor.RED, message, NamedTextColor.GRAY);
    }

    private static Component prefixed(String title, NamedTextColor titleColor, String message, NamedTextColor messageColor) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(title == null || title.isBlank() ? "OpenAccess" : title, titleColor, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message == null ? "" : message, messageColor))
                .decoration(TextDecoration.ITALIC, false);
    }
}
