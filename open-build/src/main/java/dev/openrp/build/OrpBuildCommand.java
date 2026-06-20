package dev.openrp.build;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementa {@code /orp-build export|import|info}.
 *
 * <p><b>export</b> salva la selezione WorldEdit del giocatore come
 * {@code region.schem} e genera uno scheletro di {@code build.yml} con anchor e
 * size gia' compilati: cosi' il contributore deve solo aggiungere licenza e
 * descrizione e aprire la PR. <b>import</b> e' il lato deploy: legge il manifest
 * approvato e incolla lo schematic all'anchor/rotazione indicati.</p>
 */
public final class OrpBuildCommand implements CommandExecutor, TabCompleter {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    private final OpenBuildPlugin plugin;
    private final boolean worldEditPresent;

    OrpBuildCommand(OpenBuildPlugin plugin, boolean worldEditPresent) {
        this.plugin = plugin;
        this.worldEditPresent = worldEditPresent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Uso: /orp-build <export|import|info> <slug>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "export" -> handleExport(sender, args);
            case "import" -> handleImport(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sender.sendMessage("Sottocomando sconosciuto: " + sub);
        }
        return true;
    }

    private boolean requireWorldEdit(CommandSender sender) {
        if (!worldEditPresent) {
            sender.sendMessage("WorldEdit non e' installato: export/import non disponibili.");
            return false;
        }
        return true;
    }

    private boolean validSlug(CommandSender sender, String slug) {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            sender.sendMessage("Slug non valido: usa lettere minuscole, numeri, '-' o '_'.");
            return false;
        }
        return true;
    }

    // --- export: selezione WorldEdit -> region.schem + build.yml ---
    private void handleExport(CommandSender sender, String[] args) {
        if (!requireWorldEdit(sender)) {
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un giocatore puo' esportare una selezione.");
            return;
        }
        if (args.length < 2 || !validSlug(sender, args.length >= 2 ? args[1] : null)) {
            sender.sendMessage("Uso: /orp-build export <slug>");
            return;
        }
        String slug = args[1];

        WorldEditPlugin we = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        LocalSession session = we.getSession(player);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());

        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            player.sendMessage("Seleziona prima un'area con WorldEdit (//pos1, //pos2).");
            return;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(min);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, min);
            Operations.complete(copy);
        } catch (Exception e) {
            player.sendMessage("Errore durante la copia: " + e.getMessage());
            return;
        }

        File outDir = new File(resolve(plugin.getConfig().getString("output-dir", "plugins/OpenBuild/out")), slug);
        if (!outDir.exists() && !outDir.mkdirs()) {
            player.sendMessage("Impossibile creare la cartella " + outDir.getPath());
            return;
        }
        File schemFile = new File(outDir, "region.schem");
        try (FileOutputStream fos = new FileOutputStream(schemFile);
             ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(fos)) {
            writer.write(clipboard);
        } catch (IOException e) {
            player.sendMessage("Errore salvando lo schematic: " + e.getMessage());
            return;
        }

        int sx = region.getWidth();
        int sy = region.getHeight();
        int sz = region.getLength();
        String manifest = renderManifest(slug, player.getName(), player.getWorld().getName(),
                min.x(), min.y(), min.z(), sx, sy, sz);
        try {
            Files.writeString(new File(outDir, "build.yml").toPath(), manifest, StandardCharsets.UTF_8);
        } catch (IOException e) {
            player.sendMessage("Schematic salvato, ma errore scrivendo build.yml: " + e.getMessage());
            return;
        }

        player.sendMessage("Esportato '" + slug + "' in " + outDir.getPath());
        player.sendMessage("Anchor " + min.x() + "," + min.y() + "," + min.z()
                + "  size " + sx + "x" + sy + "x" + sz + ". Aggiungi licenza e apri la PR verso dev.");
    }

    // --- import: build.yml approvato -> paste nel mondo ---
    private void handleImport(CommandSender sender, String[] args) {
        if (!requireWorldEdit(sender)) {
            return;
        }
        if (args.length < 2 || !validSlug(sender, args.length >= 2 ? args[1] : null)) {
            sender.sendMessage("Uso: /orp-build import <slug>");
            return;
        }
        String slug = args[1];

        File buildDir = new File(resolve(plugin.getConfig().getString("builds-dir", "community-server/builds")), slug);
        File manifestFile = new File(buildDir, "build.yml");
        File schemFile = new File(buildDir, "region.schem");
        if (!manifestFile.isFile() || !schemFile.isFile()) {
            sender.sendMessage("Costruzione '" + slug + "' non trovata in " + buildDir.getPath());
            return;
        }

        YamlConfiguration manifest = YamlConfiguration.loadConfiguration(manifestFile);
        String worldName = manifest.getString("world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("Mondo '" + worldName + "' non caricato sul server.");
            return;
        }
        BlockVector3 anchor = BlockVector3.at(
                manifest.getInt("anchor.x"), manifest.getInt("anchor.y"), manifest.getInt("anchor.z"));
        int rotation = ((manifest.getInt("rotation", 0) % 360) + 360) % 360;
        boolean pasteAir = plugin.getConfig().getBoolean("paste-air", false);

        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) {
            sender.sendMessage("Formato schematic non riconosciuto: " + schemFile.getName());
            return;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (FileInputStream fis = new FileInputStream(schemFile);
             ClipboardReader reader = format.getReader(fis)) {
            Clipboard clipboard = reader.read();
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                if (rotation != 0) {
                    holder.setTransform(new AffineTransform().rotateY(rotation));
                }
                Operation operation = holder.createPaste(editSession)
                        .to(anchor)
                        .ignoreAirBlocks(!pasteAir)
                        .build();
                Operations.complete(operation);
            }
        } catch (Exception e) {
            sender.sendMessage("Errore incollando '" + slug + "': " + e.getMessage());
            return;
        }

        sender.sendMessage("Incollata '" + slug + "' in " + worldName + " @ "
                + anchor.x() + "," + anchor.y() + "," + anchor.z() + " (rot " + rotation + ").");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Uso: /orp-build info <slug>");
            return;
        }
        File buildDir = new File(resolve(plugin.getConfig().getString("builds-dir", "community-server/builds")), args[1]);
        File manifestFile = new File(buildDir, "build.yml");
        if (!manifestFile.isFile()) {
            sender.sendMessage("Nessun manifest in " + buildDir.getPath());
            return;
        }
        YamlConfiguration m = YamlConfiguration.loadConfiguration(manifestFile);
        sender.sendMessage("Costruzione '" + args[1] + "': mondo " + m.getString("world")
                + ", anchor " + m.getInt("anchor.x") + "," + m.getInt("anchor.y") + "," + m.getInt("anchor.z")
                + ", rot " + m.getInt("rotation", 0));
    }

    /** Risolve un path relativo rispetto alla working dir del server. */
    private File resolve(String path) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(Bukkit.getWorldContainer(), path);
    }

    private static String renderManifest(String id, String author, String world,
                                         int ax, int ay, int az, int sx, int sy, int sz) {
        return """
                # Generato da /orp-build export. Completa licenza/descrizione prima della PR.
                id: %s
                author: %s
                world: %s
                anchor: { x: %d, y: %d, z: %d }
                size: { x: %d, y: %d, z: %d }
                rotation: 0
                license: CC-BY-4.0
                attribution: ""
                description: ""
                depends_on: []
                """.formatted(id, author, world, ax, ay, az, sx, sy, sz);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("export", "import", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
