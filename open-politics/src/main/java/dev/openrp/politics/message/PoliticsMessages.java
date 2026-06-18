package dev.openrp.politics.message;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds and sends user-facing messages. Chat lines are prefixed and colour-coded by severity; text is
 * exposed both as raw (placeholder-substituted) strings and as deserialized MiniMessage components.
 * Keeping all presentation here means the core/services never format text themselves.
 */
public final class PoliticsMessages {

    private final JavaPlugin plugin;
    private final LanguageService languageService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private Component prefix = Component.text("[Politica] ", NamedTextColor.GOLD);

    public PoliticsMessages(JavaPlugin plugin, LanguageService languageService) {
        this.plugin = plugin;
        this.languageService = languageService;
    }

    public void reload() {
        String rawPrefix = plugin.getConfig().getString("branding.message-prefix", "<gold>[Politica]</gold> ");
        this.prefix = miniMessage.deserialize(rawPrefix);
    }

    public void success(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(line(sender, NamedTextColor.GREEN, key, placeholders));
    }

    public void error(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(line(sender, NamedTextColor.RED, key, placeholders));
    }

    public void warning(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(line(sender, NamedTextColor.YELLOW, key, placeholders));
    }

    public void info(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(line(sender, NamedTextColor.AQUA, key, placeholders));
    }

    /** Sends a raw, prefixed line whose value may contain its own MiniMessage colours (for lists). */
    public void plain(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(prefix.append(mini(sender, key, placeholders)));
    }

    /** Raw, placeholder-substituted text (may contain MiniMessage tags). */
    public String text(CommandSender sender, String key, Object... placeholders) {
        return languageService.text(sender, key, placeholderMap(placeholders));
    }

    /** Deserialized MiniMessage component, italics off. */
    public Component mini(CommandSender sender, String key, Object... placeholders) {
        return miniMessage.deserialize(text(sender, key, placeholders)).decoration(TextDecoration.ITALIC, false);
    }

    private Component line(CommandSender sender, TextColor color, String key, Object... placeholders) {
        return prefix.append(Component.text(text(sender, key, placeholders), color)
                .decoration(TextDecoration.ITALIC, false));
    }

    private Map<String, ?> placeholderMap(Object... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return Map.of();
        }
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result.put(String.valueOf(placeholders[i]), placeholders[i + 1]);
        }
        return result;
    }
}
