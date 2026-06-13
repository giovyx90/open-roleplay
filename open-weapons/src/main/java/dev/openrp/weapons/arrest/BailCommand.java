package dev.openrp.weapons.arrest;

import it.meridian.bank.database.AccountDAO;
import it.meridian.bank.model.AccountType;
import it.meridian.bank.model.BankAccount;
import it.meridian.bank.module.BankModule;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BailCommand implements CommandExecutor {
    private final WeaponsModule module;

    public BailCommand(WeaponsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        ArrestRecord record = module.getArrestManager().getRecord(player.getUniqueId());
        if (record == null) {
            player.sendMessage(Component.text("Non sei arrestato.", NamedTextColor.RED));
            return true;
        }

        if (record.getBailAmount() <= 0) {
            player.sendMessage(Component.text("Non e' stata impostata una cauzione per il tuo arresto. Devi scontare la pena.", NamedTextColor.RED));
            return true;
        }

        // Get BankModule
        BankModule bankModule = module.getCore().getModuleManager().getModule(BankModule.class);
        if (bankModule == null || !bankModule.isDatabaseReady()) {
            player.sendMessage(Component.text("Il sistema bancario non e' disponibile.", NamedTextColor.RED));
            return true;
        }

        AccountDAO accountDAO = bankModule.getAccountDAO();

        // Get the player's display name from CityHall for bank account lookup
        String accountName = getAccountName(player);

        accountDAO.getAccount(accountName, AccountType.PLAYER).thenAccept(account -> {
            if (account == null) {
                player.sendMessage(Component.text("Non hai un conto bancario.", NamedTextColor.RED));
                return;
            }

            if (account.getBalance() < record.getBailAmount()) {
                player.sendMessage(Component.text("Fondi insufficienti! Ti servono $" + String.format("%.2f", record.getBailAmount())
                        + ", ma hai solo $" + String.format("%.2f", account.getBalance()) + ".", NamedTextColor.RED));
                return;
            }

            // Deduct bail
            account.setAvailableBalance(account.getBalance() - record.getBailAmount());
            account.setAccountingBalance(account.getBalance());
            accountDAO.updateAccount(account).thenRun(() -> {
                module.getCore().getServer().getScheduler().runTask(module.getCore(), () -> {
                    module.getArrestManager().release(player.getUniqueId(), "Cauzione pagata ($" + String.format("%.2f", record.getBailAmount()) + ")");
                    player.sendMessage(Component.text("Cauzione pagata! $" + String.format("%.2f", record.getBailAmount()) + " sono stati scalati dal tuo conto.", NamedTextColor.GREEN));
                });
            });
        }).exceptionally(ex -> {
            player.sendMessage(Component.text("Si e' verificato un errore durante il pagamento della cauzione.", NamedTextColor.RED));
            ex.printStackTrace();
            return null;
        });

        return true;
    }

    private String getAccountName(Player player) {
        // Try to get identity name from CityHall for bank account lookup
        try {
            var cityHall = module.getCore().getModuleManager().getModule(it.meridian.cityhall.module.CityHallModule.class);
            if (cityHall != null && cityHall.getNameTagHandler() != null) {
                var identity = cityHall.getNameTagHandler().getIdentity(player.getUniqueId());
                if (identity != null) {
                    return identity.getFullName();
                }
            }
        } catch (Exception ignored) {}
        return player.getName();
    }
}
