package dev.openrp.crime.module.production;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.LocationType;
import dev.openrp.crime.config.Recipe;
import dev.openrp.crime.config.RecipeIngredient;
import dev.openrp.crime.config.RecipeStage;
import dev.openrp.crime.config.Good;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.core.Ids;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.ProductionProcess;
import dev.openrp.crime.model.ProductionStatus;

/**
 * Drives illegal-good production. A process is one stage of a recipe run at a physical location: the
 * worker must be present, supply the real inputs, and come back to collect. Multi-stage recipes are
 * chained by starting the next stage with the previous stage's output. Nothing is auto-produced; the
 * plugin only times the work and records that a crime took place.
 */
public final class ProductionService {

    private final OpenCrimePlugin plugin;
    private final java.util.Map<String, ProductionProcess> byId = new ConcurrentHashMap<>();
    private final Set<String> physicalNotices = ConcurrentHashMap.newKeySet();

    public ProductionService(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byId.clear();
        physicalNotices.clear();
        for (ProductionProcess process : plugin.adapters().storage().loadProduction()) {
            byId.put(process.id(), process);
        }
    }

    public CrimeResult start(Player player, String recipeId, String stageId) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(player.getUniqueId(), Capability.PRODUCE)) {
            return CrimeResult.fail("general.no_capability");
        }
        if (plugin.config().settings().territoryRequireWorldguard() && !plugin.adapters().region().available()) {
            return CrimeResult.fail("territory.requires_worldguard");
        }
        Optional<String> region = plugin.adapters().region().regionAt(player.getLocation());
        if (region.isEmpty()) {
            return CrimeResult.fail("territory.no_region");
        }
        Recipe recipe = plugin.config().production().recipe(recipeId).orElse(null);
        if (recipe == null) {
            return CrimeResult.fail("production.unknown_recipe", "recipe", String.valueOf(recipeId));
        }
        RecipeStage stage = (stageId == null || stageId.isBlank())
                ? recipe.firstStage().orElse(null)
                : recipe.stage(stageId).orElse(null);
        if (stage == null) {
            return CrimeResult.fail("production.no_stages", "recipe", recipeId);
        }
        LocationType locationType = plugin.config().production().locationType(stage.locationType()).orElse(null);
        if (locationType == null) {
            return CrimeResult.fail("production.unknown_location", "type", stage.locationType());
        }
        if (!plugin.adapters().region().hasTag(region.get(), locationType.regionTag())) {
            return CrimeResult.fail("production.wrong_location", "type", locationType.displayName());
        }
        if (activeInRegion(region.get(), stage.locationType()) >= locationType.maxConcurrent()) {
            return CrimeResult.fail("production.location_busy", "type", locationType.displayName());
        }
        // Surface a config typo (unresolvable material) instead of silently skipping the ingredient.
        String badMaterial = firstUnresolvedMaterial(stage);
        if (badMaterial != null) {
            return CrimeResult.fail("production.bad_material", "material", badMaterial);
        }
        if (!hasInputs(player, stage)) {
            return CrimeResult.fail("production.missing_inputs");
        }
        consumeInputs(player, stage);

        Location loc = player.getLocation();
        long started = System.currentTimeMillis();
        long expected = started + plugin.config().settings().realMillisFromMinutes(stage.durationMinutes());
        ProductionProcess process = new ProductionProcess(Ids.prefixed("prod"), org.id(), recipe.id(),
                stage.id(), region.get(), player.getUniqueId(), loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), started, expected);

        CrimeEvent event = new CrimeEvent(Ids.prefixed("evt"), CrimeEventType.PRODUCTION, org.id(),
                List.of(player.getUniqueId()), List.of(), loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), started, null);
        plugin.events().register(event);
        process.setEventId(event.id());

        byId.put(process.id(), process);
        plugin.adapters().storage().saveProduction(process);
        long minutes = Math.max(1L, plugin.config().settings().realMillisFromMinutes(stage.durationMinutes()) / 60_000L);
        return CrimeResult.ok("production.started", "recipe", recipe.id(), "stage", stage.id(),
                "minutes", String.valueOf(minutes));
    }

    public CrimeResult collect(Player player) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        Optional<String> region = plugin.adapters().region().regionAt(player.getLocation());
        if (region.isEmpty()) {
            return CrimeResult.fail("territory.no_region");
        }
        long now = System.currentTimeMillis();
        ProductionProcess ready = null;
        boolean pending = false;
        for (ProductionProcess process : byId.values()) {
            if (!process.orgId().equals(org.id()) || !process.locationRegion().equals(region.get())
                    || process.status() != ProductionStatus.ACTIVE) {
                continue;
            }
            if (process.isElapsed(now)) {
                ready = process;
                break;
            }
            pending = true;
        }
        if (ready == null) {
            return pending ? CrimeResult.fail("production.not_ready") : CrimeResult.fail("production.none_here");
        }
        Recipe recipe = plugin.config().production().recipe(ready.recipeId()).orElse(null);
        RecipeStage stage = recipe == null ? null : recipe.stage(ready.stageId()).orElse(null);
        if (stage == null) {
            byId.remove(ready.id());
            plugin.adapters().storage().deleteProduction(ready.id());
            return CrimeResult.fail("production.recipe_gone");
        }
        int quality = rollQuality();
        giveOutputs(player, stage, quality, org);
        ready.setStatus(ProductionStatus.COMPLETED);
        byId.remove(ready.id());
        plugin.adapters().storage().deleteProduction(ready.id());
        return CrimeResult.ok("production.collected", "recipe", recipe.id(), "quality", String.valueOf(quality));
    }

    public CrimeResult cancel(Player player) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(player.getUniqueId(), Capability.PRODUCE_CANCEL)) {
            return CrimeResult.fail("general.no_capability");
        }
        Optional<String> region = plugin.adapters().region().regionAt(player.getLocation());
        if (region.isEmpty()) {
            return CrimeResult.fail("territory.no_region");
        }
        for (ProductionProcess process : byId.values()) {
            if (process.orgId().equals(org.id()) && process.locationRegion().equals(region.get())
                    && process.status() == ProductionStatus.ACTIVE) {
                byId.remove(process.id());
                plugin.adapters().storage().deleteProduction(process.id());
                return CrimeResult.ok("production.cancelled");
            }
        }
        return CrimeResult.fail("production.none_here");
    }

    public List<ProductionProcess> ofOrg(String orgId) {
        List<ProductionProcess> result = new ArrayList<>();
        for (ProductionProcess process : byId.values()) {
            if (process.orgId().equals(orgId)) {
                result.add(process);
            }
        }
        return result;
    }

    public long remainingMinutes(ProductionProcess process) {
        long remaining = process.expectedAt() - System.currentTimeMillis();
        return remaining <= 0 ? 0 : Math.max(1L, remaining / 60_000L);
    }

    /**
     * Physical-discovery scan: an agent standing within a discoverable location's radius learns about
     * the active production right there, right then. Registers a {@code scoperta_fisica} discovery with
     * no dossier - the agent must still act in RP; nothing happens remotely or automatically.
     */
    public void scanPhysicalDiscovery() {
        if (byId.isEmpty()) {
            return;
        }
        for (ProductionProcess process : byId.values()) {
            if (process.status() != ProductionStatus.ACTIVE || process.eventId() == null) {
                continue;
            }
            Recipe recipe = plugin.config().production().recipe(process.recipeId()).orElse(null);
            RecipeStage stage = recipe == null ? null : recipe.stage(process.stageId()).orElse(null);
            LocationType type = stage == null ? null
                    : plugin.config().production().locationType(stage.locationType()).orElse(null);
            if (type == null || !type.discoverable()) {
                continue;
            }
            org.bukkit.World world = plugin.getServer().getWorld(process.world());
            if (world == null) {
                continue;
            }
            Location anchor = new Location(world, process.x(), process.y(), process.z());
            double radius = type.discoveryRadius();
            for (Player agent : world.getNearbyPlayers(anchor, radius)) {
                if (!plugin.adapters().authority().isAgent(agent)) {
                    continue;
                }
                String token = agent.getUniqueId() + "|" + process.eventId();
                if (!physicalNotices.add(token)) {
                    continue;
                }
                CrimeEvent event = plugin.events().find(process.eventId()).orElse(null);
                if (event == null) {
                    continue;
                }
                // Survive a reload (which clears the in-memory notice cache): never mint a second
                // physical discovery for an event this agent already discovered.
                boolean already = plugin.discoveries().byEvent(event.id()).stream().anyMatch(d ->
                        d.type() == dev.openrp.crime.model.DiscoveryType.SCOPERTA_FISICA
                                && agent.getUniqueId().equals(d.discoveredBy()));
                if (already) {
                    continue;
                }
                plugin.discoveries().open(dev.openrp.crime.model.DiscoveryType.SCOPERTA_FISICA,
                        agent.getUniqueId(), process.world(), process.x(), process.y(), process.z(),
                        null, List.of(event));
                plugin.adapters().notification().send(agent, plugin.messages().prefixed(agent,
                        "production.agent_discovered", "type", type.displayName()));
            }
        }
    }

    private int activeInRegion(String regionId, String locationTypeId) {
        int count = 0;
        for (ProductionProcess process : byId.values()) {
            if (process.status() != ProductionStatus.ACTIVE || !process.locationRegion().equals(regionId)) {
                continue;
            }
            Recipe recipe = plugin.config().production().recipe(process.recipeId()).orElse(null);
            RecipeStage stage = recipe == null ? null : recipe.stage(process.stageId()).orElse(null);
            if (stage != null && stage.locationType().equals(locationTypeId)) {
                count++;
            }
        }
        return count;
    }

    /** The first input or plain-output material name that does not resolve to a Bukkit material, or null. */
    private String firstUnresolvedMaterial(RecipeStage stage) {
        for (RecipeIngredient input : stage.inputs()) {
            if (Material.matchMaterial(input.material()) == null) {
                return input.material();
            }
        }
        for (RecipeIngredient output : stage.outputs()) {
            if (Material.matchMaterial(output.material()) == null) {
                return output.material();
            }
        }
        return null;
    }

    private boolean hasInputs(Player player, RecipeStage stage) {
        for (RecipeIngredient input : stage.inputs()) {
            Material material = Material.matchMaterial(input.material());
            if (material == null || !player.getInventory().containsAtLeast(new ItemStack(material), input.amount())) {
                return false;
            }
        }
        return true;
    }

    private void consumeInputs(Player player, RecipeStage stage) {
        for (RecipeIngredient input : stage.inputs()) {
            Material material = Material.matchMaterial(input.material());
            if (material != null) {
                player.getInventory().removeItem(new ItemStack(material, input.amount()));
            }
        }
    }

    private void giveOutputs(Player player, RecipeStage stage, int quality, IllegalOrg org) {
        List<ItemStack> drops = new ArrayList<>();
        for (RecipeIngredient output : stage.outputs()) {
            Material material = Material.matchMaterial(output.material());
            if (material != null) {
                drops.add(new ItemStack(material, output.amount()));
            }
        }
        if (stage.yieldsGood()) {
            Good good = plugin.config().goods().get(stage.outputGood()).orElse(null);
            if (good != null) {
                drops.add(plugin.goods().create(good, stage.outputGoodAmount(), quality,
                        player.getUniqueId(), org.id()));
            }
        }
        for (ItemStack drop : drops) {
            player.getInventory().addItem(drop).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    private int rollQuality() {
        return 2 + ThreadLocalRandom.current().nextInt(0, 3);
    }

    /** Drops the transient discovery-notice cache (used on reload). */
    public void clearNotices() {
        physicalNotices.clear();
    }
}
