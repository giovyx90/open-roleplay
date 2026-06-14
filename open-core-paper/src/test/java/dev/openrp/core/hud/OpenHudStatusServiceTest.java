package dev.openrp.core.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OpenHudStatusServiceTest {
    @Test
    public void returnsActiveStatusUntilCleared() {
        OpenHudStatusService service = new OpenHudStatusService();
        Player player = player(UUID.randomUUID());

        service.show(player, Component.text("Attivo"), 20L);

        assertEquals("Attivo", PlainTextComponentSerializer.plainText().serialize(service.activeStatus(player)));
        service.clear(player);
        assertNull(service.activeStatus(player));
    }

    @Test
    public void expiresOldStatus() throws Exception {
        OpenHudStatusService service = new OpenHudStatusService();
        Player player = player(UUID.randomUUID());

        service.show(player, Component.text("Breve"), 1L);
        Thread.sleep(75L);

        assertNull(service.activeStatus(player));
    }

    private Player player(UUID uuid) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "isOnline" -> true;
                    case "toString" -> "PlayerProxy{" + uuid + "}";
                    case "hashCode" -> uuid.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
