package dev.openrp.vending.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Small helper for case-insensitive, sorted, prefix-filtered tab completion. */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    public static List<String> filter(Collection<String> values, String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value != null && value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .distinct()
                .sorted()
                .limit(60)
                .toList();
    }
}
