package dev.openrp.companies.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Small helper for case-insensitive prefix filtering of tab-completion suggestions. */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    /** Returns the entries of {@code options} that start with {@code token} (case-insensitive). */
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
