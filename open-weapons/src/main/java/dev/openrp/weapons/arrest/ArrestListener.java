package dev.openrp.weapons.arrest;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ArrestListener implements Listener {
    private final WeaponsModule module;

    public ArrestListener(WeaponsModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ArrestRecord record = module.getArrestManager().getRecord(player.getUniqueId());
        if (record == null) return;

        // Only check if they actually moved blocks (not just looking around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Check if still inside jail region
        if (!module.getArrestManager().isInJailRegion(player, record.getJailRegionId())) {
            // Teleport back to jail center
            org.bukkit.Location jailLoc = module.getArrestManager().getRegionCenter(record.getJailRegionId());
            if (jailLoc != null) {
                event.setCancelled(true);
                player.teleport(jailLoc);
            }
            player.sendMessage(Component.text("Non puoi lasciare la cella! Tempo rimanente: " + record.getRemainingFormatted(), NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ArrestRecord record = module.getArrestManager().getRecord(player.getUniqueId());
        if (record == null) return;

        if (record.isExpired()) {
            module.getArrestManager().release(player.getUniqueId(), "Pena scontata");
        } else {
            // Ensure they are uncuffed when they enter jail (e.g. if arrested offline)
            module.getHandcuffManager().uncuff(player);
            
            // Teleport back to jail
            org.bukkit.Location jailLoc = module.getArrestManager().getRegionCenter(record.getJailRegionId());
            if (jailLoc != null) {
                player.teleport(jailLoc);
            }
            player.sendMessage(Component.text("Sei ancora arrestato! Tempo rimanente: " + record.getRemainingFormatted(), NamedTextColor.RED));
            if (record.getBailAmount() > 0) {
                player.sendMessage(Component.text("Usa /bail per pagare $" + String.format("%.2f", record.getBailAmount()) + " e venire rilasciato.", NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Handle GUI chat input for arrest configuration
        if (module.getArrestGUI().handleChatInput(player, event.getMessage())) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!module.getArrestManager().isArrested(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        // Allow only /bail and /msg-type commands while arrested
        if (cmd.equals("/bail") || cmd.equals("/msg") || cmd.equals("/r") || cmd.equals("/sms") || cmd.equals("/w")) {
            return;
        }

        if (!player.hasPermission("openrp.weapons.arrest.bypass")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Non puoi usare comandi mentre sei arrestato! Usa /bail per pagare la cauzione.", NamedTextColor.RED));
        }
    }
}
