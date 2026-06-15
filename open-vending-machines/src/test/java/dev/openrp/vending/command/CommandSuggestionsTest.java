package dev.openrp.vending.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class CommandSuggestionsTest {

    @Test
    public void filtersCaseInsensitiveAndSorted() {
        assertEquals(List.of("create"), CommandSuggestions.filter(List.of("create", "remove", "list"), "cr"));
        assertEquals(List.of("a", "b", "c"), CommandSuggestions.filter(List.of("c", "a", "b"), null));
    }

    @Test
    public void rootSubcommandsExposeEveryAction() {
        assertTrue(OpenVendingCommand.rootSubcommands().containsAll(
                List.of("help", "create", "remove", "list", "info", "restock", "withdraw", "reload", "giveitem")));
    }
}
