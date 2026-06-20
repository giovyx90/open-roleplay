package dev.openrp.build;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Open Build: strumento del server faro per portare le costruzioni dentro al
 * flusso "Costruisci -> fatti approvare -> entra in gioco".
 *
 * <p>Espone {@code /orp-build export <slug>} per salvare la selezione WorldEdit
 * come {@code region.schem} + uno scheletro di {@code build.yml} pronto per la
 * pull request, e {@code /orp-build import <slug>} per incollare in produzione
 * una costruzione approvata leggendo anchor/rotazione dal suo manifest.</p>
 *
 * <p>WorldEdit e' una soft-dependency: se manca, i comandi rispondono con un
 * messaggio chiaro invece di rompersi (degradazione pulita, come gli altri
 * moduli Open Roleplay).</p>
 */
public final class OpenBuildPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        boolean worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
        if (!worldEdit) {
            getLogger().warning("WorldEdit non trovato: i comandi export/import resteranno disattivati "
                    + "finche' non lo installi. Il plugin si avvia comunque.");
        }

        OrpBuildCommand command = new OrpBuildCommand(this, worldEdit);
        var registered = getCommand("orp-build");
        if (registered != null) {
            registered.setExecutor(command);
            registered.setTabCompleter(command);
        }

        getLogger().info("Open Build pronto" + (worldEdit ? "." : " (modalita' limitata, manca WorldEdit)."));
    }
}
