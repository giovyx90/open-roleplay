package dev.openrp.vending.message;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads messages_en.yml / messages_it.yml and resolves a key for a sender, honouring the player's
 * client locale in {@code auto} mode and falling back gracefully. Placeholder substitution uses the
 * {@code {name}} syntax. Mirrors the language handling of other Open Roleplay modules.
 */
public final class LanguageService {

    public static final String EN = "en";
    public static final String IT = "it";

    private final JavaPlugin plugin;
    private String mode = "auto";
    private String fallback = EN;
    private YamlConfiguration english = new YamlConfiguration();
    private YamlConfiguration italian = new YamlConfiguration();

    public LanguageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.mode = plugin.getConfig().getString("messages.language", "auto").toLowerCase(Locale.ROOT);
        this.fallback = normalizeLanguage(plugin.getConfig().getString("messages.fallback", EN));
        this.english = load("messages_en.yml");
        this.italian = load("messages_it.yml");
    }

    public String resolveLanguage(CommandSender sender) {
        if (!"auto".equals(mode)) {
            return normalizeLanguage(mode);
        }
        if (sender instanceof Player player) {
            return fromLocale(player.locale());
        }
        return fallback;
    }

    public String text(CommandSender sender, String key) {
        String language = resolveLanguage(sender);
        String value = config(language).getString(key);
        if (value != null) {
            return value;
        }
        value = config(fallback).getString(key);
        if (value != null) {
            return value;
        }
        value = english.getString(key);
        return value == null ? key : value;
    }

    public String text(CommandSender sender, String key, Map<String, ?> placeholders) {
        String value = text(sender, key);
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return value;
    }

    public static String fromLocale(Locale locale) {
        if (locale == null) {
            return EN;
        }
        return locale.toString().toLowerCase(Locale.ROOT).startsWith("it") ? IT : EN;
    }

    private YamlConfiguration load(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    private YamlConfiguration config(String language) {
        return IT.equals(language) ? italian : english;
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return EN;
        }
        return value.trim().toLowerCase(Locale.ROOT).startsWith("it") ? IT : EN;
    }
}
