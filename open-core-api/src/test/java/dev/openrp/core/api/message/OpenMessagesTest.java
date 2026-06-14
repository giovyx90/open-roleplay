package dev.openrp.core.api.message;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenMessagesTest {
    @Test
    public void usesDefaultTitleWhenBlank() {
        String text = PlainTextComponentSerializer.plainText()
                .serialize(OpenMessages.info("", "Pronto"));

        assertEquals("[OpenRoleplay] Pronto", text);
    }
}
