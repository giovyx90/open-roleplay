package dev.openrp.fdo.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Small helper to filter tab-completion candidates by the partial token the player has typed. */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    public static List<String> filter(List<String> options, String token) {
        if (token == null || token.isEmpty()) {
            return options;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
