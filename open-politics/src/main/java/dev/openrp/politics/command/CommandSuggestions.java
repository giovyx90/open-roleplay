package dev.openrp.politics.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Tab-completion helper: case-insensitive prefix filtering. */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    public static List<String> filter(List<String> options, String token) {
        if (token == null || token.isEmpty()) {
            return List.copyOf(options);
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
