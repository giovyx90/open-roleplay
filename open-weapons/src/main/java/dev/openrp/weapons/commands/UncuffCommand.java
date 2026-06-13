package dev.openrp.weapons.commands;

import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.utility.StatusTextDisplays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UncuffCommand implements CommandExecutor, Listener {
    private final WeaponsModule module;
    private final Map<UUID, BukkitTask> activeAttempts = new HashMap<>(); // FDO UUID -> Task
    private final Map<UUID, org.bukkit.entity.TextDisplay> killableTags = new HashMap<>();

    public UncuffCommand(WeaponsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("openrp.weapons.uncuff")) {
            player.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return true;
        }

        if (module.getHandcuffManager().isRestrained(player)) {
            player.sendMessage(Component.text("You cannot do this while restrained!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /uncuff <handcuffed_player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        if (!module.getHandcuffManager().isHandcuffed(target)) {
            player.sendMessage(Component.text("This player is not handcuffed.", NamedTextColor.RED));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);

        if (weapon == null || weapon.getCategory() == WeaponCategory.MELEE) {
            player.sendMessage(Component.text("You must hold a firearm to threaten the officer.", NamedTextColor.RED));
            return true;
        }

        UUID officerId = module.getHandcuffManager().getOfficerWhoCuffed(target);
        if (officerId == null) {
            player.sendMessage(Component.text("The officer who handcuffed them is no longer online.", NamedTextColor.RED));
            return true;
        }

        Player officer = Bukkit.getPlayer(officerId);
        if (officer == null || !officer.isOnline()) {
            player.sendMessage(Component.text("The officer who handcuffed them is no longer online.", NamedTextColor.RED));
            return true;
        }

        if (player.getLocation().distance(officer.getLocation()) > 15) {
            player.sendMessage(Component.text("You are too far from the officer to threaten them!", NamedTextColor.RED));
            return true;
        }

        if (activeAttempts.containsKey(officer.getUniqueId())) {
            player.sendMessage(Component.text("The officer is already being threatened!", NamedTextColor.RED));
            return true;
        }

        startIntimidation(player, officer, target);
        return true;
    }

    private void startIntimidation(Player sender, Player officer, Player victim) {
        sender.sendMessage(Component.text("You ordered the officer to free " + victim.getName() + "!", NamedTextColor.YELLOW));
        officer.sendMessage(Component.text("You are under armed threat! You have 15 seconds to uncuff " + victim.getName() + " or you will be KILLABLE!", NamedTextColor.RED, TextDecoration.BOLD));

        // "UNCUFF ME!" above the handcuffed criminal (victim)
        Component uncuffTag = Component.text("⚠ UNCUFF ME! ⚠", NamedTextColor.RED, TextDecoration.BOLD);
        double tagOffset = module.getUtilitySettings().statusTagYOffset();
        org.bukkit.entity.TextDisplay victimTd = StatusTextDisplays.spawn(victim, uncuffTag, tagOffset);

        // Timer countdown above the officer
        org.bukkit.entity.TextDisplay officerTd = StatusTextDisplays.spawn(officer,
                Component.text("⏱ 15s", NamedTextColor.YELLOW, TextDecoration.BOLD), tagOffset);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 15 * 20;

            private void cleanup() {
                if (victimTd.isValid()) victimTd.remove();
                if (officerTd.isValid()) officerTd.remove();
                cancelAttempt(officer.getUniqueId());
            }

            @Override
            public void run() {
                if (!officer.isOnline() || officer.isDead()) {
                    cleanup();
                    return;
                }

                if (!module.getHandcuffManager().isHandcuffed(victim)) {
            officer.sendMessage(Component.text("You uncuffed them and are now safe.", NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("The officer complied.", NamedTextColor.GREEN));
                    cleanup();
                    return;
                }

                if (!sender.isOnline() || sender.isDead()) {
                    cleanup();
                    return;
                }

                ItemStack item = sender.getInventory().getItemInMainHand();
                WeaponDefinition weapon = module.getWeaponRegistry().getWeapon(item);
                if (weapon == null || weapon.getCategory() == WeaponCategory.MELEE) {
                    sender.sendMessage(Component.text("You lowered your weapon! Threat cancelled.", NamedTextColor.RED));
                    officer.sendMessage(Component.text("The attacker lowered their weapon.", NamedTextColor.GREEN));
                    cleanup();
                    return;
                }

                int secondsLeft = (maxTicks - ticks) / 20;

                Component timerTag = Component.text(module.message("uncuff.ultimatum_timer", "⏱ Ultimatum... {seconds}s").replace("{seconds}", String.valueOf(secondsLeft)), NamedTextColor.YELLOW, TextDecoration.BOLD);

                // Track victim TextDisplay
                if (!victim.isDead() && victimTd.isValid()) {
                    StatusTextDisplays.follow(victimTd, victim, tagOffset);
                }

                // Track officer TextDisplay with countdown
                if (!officer.isDead() && officerTd.isValid()) {
                    officerTd.text(timerTag);
                    StatusTextDisplays.follow(officerTd, officer, tagOffset);
                }

                if (ticks % 20 == 0) {
                    officer.getWorld().playSound(officer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                }

                if (ticks >= maxTicks) {
                    if (victimTd.isValid()) victimTd.remove();
                    if (officerTd.isValid()) officerTd.remove();
                    completeIntimidation(officer);
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(module.getCore(), 0L, 1L);

        activeAttempts.put(officer.getUniqueId(), task);
    }

    private void cancelAttempt(UUID officerId) {
        BukkitTask task = activeAttempts.remove(officerId);
        if (task != null) task.cancel();

        Player officer = Bukkit.getPlayer(officerId);
        if (officer != null && officer.isOnline()) {
            refreshEntityForOthers(officer);
        }
    }

    private void completeIntimidation(Player officer) {
        BukkitTask task = activeAttempts.remove(officer.getUniqueId());
        if (task != null) task.cancel();

        officer.sendMessage(Component.text("Time's up! You are now KILLABLE for 5 minutes.", NamedTextColor.RED, TextDecoration.BOLD));
        officer.getWorld().playSound(officer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);

        applyKillableTag(officer);
    }

    private void applyKillableTag(Player officer) {
        final int KILLABLE_SECONDS = 300; // 5 minutes
        final int KILLABLE_TICKS = KILLABLE_SECONDS * 20;

        double tagOffset = module.getUtilitySettings().statusTagYOffset();
        org.bukkit.entity.TextDisplay td = StatusTextDisplays.spawn(officer,
                Component.text("☠ KILLABLE - 5:00 ☠", NamedTextColor.DARK_RED, TextDecoration.BOLD), tagOffset);
        
        killableTags.put(officer.getUniqueId(), td);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= KILLABLE_TICKS || !officer.isOnline()) {
                    if (td.isValid()) td.remove();
                    killableTags.remove(officer.getUniqueId());
                    if (officer.isOnline()) {
                        officer.sendMessage(Component.text("You are no longer killable.", NamedTextColor.GREEN));
                    }
                    this.cancel();
                    return;
                }
                if (!officer.isDead() && td.isValid()) {
                    int secondsLeft = (KILLABLE_TICKS - ticks) / 20;
                    int mins = secondsLeft / 60;
                    int secs = secondsLeft % 60;
                    td.text(Component.text("☠ KILLABLE - " + mins + ":" + String.format("%02d", secs) + " ☠", NamedTextColor.DARK_RED, TextDecoration.BOLD));
                    StatusTextDisplays.follow(td, officer, tagOffset);
                }
                ticks++;
            }
        }.runTaskTimer(module.getCore(), 0L, 1L);
    }

    private void restoreName(Player player) {
        it.meridian.cityhall.module.CityHallModule cityHall = module.getCore().getModuleManager().getModule(it.meridian.cityhall.module.CityHallModule.class);
        if (cityHall != null && cityHall.getNameTagHandler() != null) {
            cityHall.getNameTagHandler().refreshPlayer(player);
        } else {
            player.setCustomNameVisible(false);
            refreshEntityForOthers(player);
        }
    }

    private void refreshEntityForOthers(Player player) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player) && p.canSee(player)) {
                p.hidePlayer(module.getCore(), player);
                p.showPlayer(module.getCore(), player);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
        }
    }
}
