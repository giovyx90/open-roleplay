package dev.openrp.access;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.openrp.OpenAccessPlugin;
import dev.openrp.access.api.OpenAccessApi;
import dev.openrp.access.api.OpenAccessOwnerProvider;
import dev.openrp.access.gui.AccessEditorGUI;
import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessCheckResult;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessPrincipalType;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessProfileType;
import dev.openrp.access.model.AccessRule;
import dev.openrp.access.model.AccessRuleScope;
import dev.openrp.access.storage.AccessStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccessService implements OpenAccessApi {
    private final OpenAccessPlugin plugin;
    private final AccessStorage storage;
    private final AccessResolver resolver;
    private final AccessProviderRegistry providerRegistry = new AccessProviderRegistry();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "open-access-storage");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Map<String, AccessProfile> profilesById = Map.of();
    private volatile Map<String, AccessProfile> profilesByRegion = Map.of();
    private volatile Map<String, List<AccessRule>> regionRulesByProfile = Map.of();
    private volatile Map<String, Map<String, List<AccessRule>>> blockRulesByProfile = Map.of();

    public AccessService(OpenAccessPlugin plugin, AccessStorage storage, AccessResolver resolver) {
        this.plugin = plugin;
        this.storage = storage;
        this.resolver = resolver;
    }

    public CompletableFuture<Void> initialize() {
        return runStorage(storage::createTables).thenCompose(ignored -> refreshCache());
    }

    public OpenAccessPlugin core() {
        return plugin;
    }

    @Override
    public CompletableFuture<Void> refreshCache() {
        return supplyStorage(() -> {
            List<AccessProfile> profiles = storage.loadProfiles();
            List<AccessRule> rules = storage.loadRules();
            Map<String, AccessProfile> nextById = new ConcurrentHashMap<>();
            Map<String, AccessProfile> nextByRegion = new ConcurrentHashMap<>();
            for (AccessProfile profile : profiles) {
                nextById.put(profile.getId(), profile);
                nextByRegion.put(profile.regionKey(), profile);
            }

            Map<String, List<AccessRule>> nextRegionRules = new ConcurrentHashMap<>();
            Map<String, Map<String, List<AccessRule>>> nextBlockRules = new ConcurrentHashMap<>();
            for (AccessRule rule : rules) {
                if (!nextById.containsKey(rule.getProfileId())) {
                    continue;
                }
                if (rule.getScope() == AccessRuleScope.REGION) {
                    nextRegionRules.computeIfAbsent(rule.getProfileId(), ignored -> new ArrayList<>()).add(rule);
                } else if (rule.getWorld() != null && rule.getX() != null && rule.getY() != null && rule.getZ() != null) {
                    String blockKey = AccessRule.blockKey(rule.getWorld(), rule.getX(), rule.getY(), rule.getZ());
                    nextBlockRules
                            .computeIfAbsent(rule.getProfileId(), ignored -> new ConcurrentHashMap<>())
                            .computeIfAbsent(blockKey, ignored -> new ArrayList<>())
                            .add(rule);
                }
            }
            this.profilesById = Map.copyOf(nextById);
            this.profilesByRegion = Map.copyOf(nextByRegion);
            this.regionRulesByProfile = freezeListMap(nextRegionRules);
            this.blockRulesByProfile = freezeNestedListMap(nextBlockRules);
            return null;
        });
    }

    @Override
    public void registerOwnerProvider(OpenAccessOwnerProvider provider) {
        providerRegistry.register(provider);
        plugin.getLogger().info("[OpenAccess] Provider registrato: " + provider.namespace());
    }

    @Override
    public void unregisterOwnerProvider(String namespace) {
        providerRegistry.unregister(namespace);
    }

    public void shutdown() {
        providerRegistry.clear();
        executor.shutdownNow();
    }

    @Override
    public AccessCheckResult resolve(Player player, Location location, AccessAction action) {
        if (player == null || location == null || action == null) {
            return AccessCheckResult.pass("Player, posizione o azione mancanti.");
        }
        Optional<AccessProfile> profileOptional = findProfileAt(location);
        if (profileOptional.isEmpty()) {
            return AccessCheckResult.pass("Nessun profilo accesso copre questa posizione.");
        }
        AccessProfile profile = profileOptional.get();
        Subject subject = subjectFor(profile, player);
        List<AccessRule> regionRules = regionRulesByProfile.getOrDefault(profile.getId(), List.of());
        List<AccessRule> blockRules = blockRulesByProfile
                .getOrDefault(profile.getId(), Map.of())
                .getOrDefault(AccessRule.blockKey(location), List.of());
        boolean managerAccess = subject.manager() || hasAllowedManageRule(profile, subject.principals());
        return resolver.resolve(profile, regionRules, blockRules, subject.principals(),
                action, AccessPermissions.hasBypass(player), managerAccess);
    }

    @Override
    public boolean canManage(Player player, AccessProfile profile) {
        if (player == null || profile == null) {
            return false;
        }
        if (AccessPermissions.hasBypass(player) || AccessPermissions.hasAdmin(player)) {
            return true;
        }
        Subject subject = subjectFor(profile, player);
        return subject.manager() || hasAllowedManageRule(profile, subject.principals());
    }

    @Override
    public Optional<AccessProfile> findProfileAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        try {
            ApplicableRegionSet regions = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(location));
            for (ProtectedRegion region : regions.getRegions()) {
                AccessProfile profile = profilesByRegion.get(AccessProfile.regionKey(location.getWorld().getName(), region.getId()));
                if (profile != null && profile.isEnabled()) {
                    return Optional.of(profile);
                }
            }
        } catch (Throwable error) {
            plugin.getLogger().warning("[OpenAccess] Lookup WorldGuard fallito: " + rootMessage(error));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AccessProfile> findProfileByRegion(String world, String regionId) {
        return Optional.ofNullable(profilesByRegion.get(AccessProfile.regionKey(world, regionId)));
    }

    public Optional<AccessProfile> findCompanyProfile(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return Optional.empty();
        }
        String wanted = companyName.trim().toLowerCase(Locale.ROOT);
        return profilesById.values().stream()
                .filter(profile -> profile.getType() == AccessProfileType.COMPANY)
                .filter(profile -> profile.getOwnerKey() != null && profile.getOwnerKey().trim().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    @Override
    public Collection<AccessProfile> profiles() {
        return profilesById.values();
    }

    public CompletableFuture<AccessProfile> linkRegion(AccessProfileType type, World world, String regionId,
                                                       String ownerArg, UUID actorUuid, String actorName) {
        if (world == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Il mondo e' obbligatorio."));
        }
        if (regionId == null || regionId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("La regione WorldGuard e' obbligatoria."));
        }
        AccessProfile existing = profilesByRegion.get(AccessProfile.regionKey(world.getName(), regionId));
        OwnerIdentity owner = resolveOwner(type, ownerArg);
        String id = existing == null ? "profile-" + UUID.randomUUID() : existing.getId();
        AccessProfile profile = new AccessProfile(id, type, world.getName(), regionId.trim(),
                owner.ownerUuid(), owner.ownerName(), owner.ownerKey(),
                displayName(type, regionId, owner), existing == null ? AccessPreset.PRIVATE : existing.getDefaultPreset(),
                true, existing == null ? Instant.now() : existing.getCreatedAt(), Instant.now());
        return runStorage(() -> {
            storage.saveProfile(profile);
            storage.audit(profile.getId(), "REGION_LINKED", actorUuid, actorName,
                    world.getName(), null, null, null, "{\"region\":\"" + json(regionId) + "\"}");
        }).thenCompose(ignored -> refreshCache()).thenApply(ignored -> profile);
    }

    public CompletableFuture<Void> unlinkRegion(AccessProfile profile, UUID actorUuid, String actorName) {
        if (profile == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Profilo accesso non trovato."));
        }
        return runStorage(() -> {
            storage.audit(profile.getId(), "REGION_UNLINKED", actorUuid, actorName,
                    profile.getWorld(), null, null, null, "{\"region\":\"" + json(profile.getRegionId()) + "\"}");
            storage.deleteProfile(profile.getId());
        }).thenCompose(ignored -> refreshCache());
    }

    public CompletableFuture<Void> setRegionPreset(AccessProfile profile, AccessPreset preset,
                                                   UUID actorUuid, String actorName) {
        if (profile == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Profilo accesso non trovato."));
        }
        AccessPreset resolved = preset == null ? AccessPreset.PRIVATE : preset;
        AccessProfile next = profile.withDefaultPreset(resolved);
        return runStorage(() -> {
            storage.saveProfile(next);
            storage.audit(profile.getId(), "REGION_PRESET_UPDATED", actorUuid, actorName,
                    profile.getWorld(), null, null, null, "{\"preset\":\"" + resolved.name() + "\"}");
        }).thenCompose(ignored -> refreshCache());
    }

    public CompletableFuture<Void> setBlockPreset(AccessProfile profile, Location location, AccessPreset preset,
                                                  UUID actorUuid, String actorName) {
        if (profile == null || location == null || location.getWorld() == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Profilo accesso e blocco sono obbligatori."));
        }
        AccessPreset resolved = preset == null ? AccessPreset.PRIVATE : preset;
        String world = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return runStorage(() -> {
            storage.deleteBlockRules(profile.getId(), world, x, y, z);
            saveBlockPresetRules(profile, world, x, y, z, resolved);
            storage.audit(profile.getId(), "BLOCK_PRESET_UPDATED", actorUuid, actorName,
                    world, x, y, z, "{\"preset\":\"" + resolved.name() + "\"}");
        }).thenCompose(ignored -> refreshCache());
    }

    public CompletableFuture<Void> addPlayerRule(AccessProfile profile, UUID playerUuid, String playerName,
                                                 Set<AccessAction> actions, UUID actorUuid, String actorName) {
        if (profile == null || playerUuid == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Profilo e UUID player sono obbligatori."));
        }
        AccessRule rule = new AccessRule(null, profile.getId(), AccessRuleScope.REGION, null, null, null, null,
                AccessPrincipal.player(playerUuid), actions == null || actions.isEmpty()
                ? EnumSet.copyOf(AccessAction.USE_ACTIONS) : actions, true, Instant.now());
        return runStorage(() -> {
            storage.saveRule(rule);
            storage.audit(profile.getId(), "PLAYER_RULE_ADDED", actorUuid, actorName,
                    profile.getWorld(), null, null, null,
                    "{\"player_uuid\":\"" + playerUuid + "\",\"player_name\":\"" + json(playerName) + "\"}");
        }).thenCompose(ignored -> refreshCache());
    }

    public CompletableFuture<Void> removePlayerRule(AccessProfile profile, UUID playerUuid,
                                                    UUID actorUuid, String actorName) {
        if (profile == null || playerUuid == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Profilo e UUID player sono obbligatori."));
        }
        return runStorage(() -> {
            storage.deletePlayerRules(profile.getId(), playerUuid.toString());
            storage.audit(profile.getId(), "PLAYER_RULE_REMOVED", actorUuid, actorName,
                    profile.getWorld(), null, null, null, "{\"player_uuid\":\"" + playerUuid + "\"}");
        }).thenCompose(ignored -> refreshCache());
    }

    @Override
    public void openEditor(Player player, AccessProfile profile, Location blockLocation) {
        if (player == null || profile == null) {
            return;
        }
        player.openInventory(new AccessEditorGUI(this, player, profile, blockLocation).getInventory());
    }

    public void applyWorldGuardFlags(World world, String regionId) {
        if (world == null || regionId == null || regionId.isBlank()) {
            return;
        }
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return;
        }
        ProtectedRegion region = manager.getRegion(regionId);
        if (region == null) {
            return;
        }
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
        region.setFlag(Flags.PASSTHROUGH, StateFlag.State.DENY);
    }

    public List<AccessRule> regionRules(AccessProfile profile) {
        if (profile == null) {
            return List.of();
        }
        return regionRulesByProfile.getOrDefault(profile.getId(), List.of());
    }

    public List<AccessRule> blockRules(AccessProfile profile, Location location) {
        if (profile == null || location == null || location.getWorld() == null) {
            return List.of();
        }
        return blockRulesByProfile
                .getOrDefault(profile.getId(), Map.of())
                .getOrDefault(AccessRule.blockKey(location), List.of());
    }

    Subject subjectFor(AccessProfile profile, Player player) {
        Set<AccessPrincipal> principals = ConcurrentHashMap.newKeySet();
        principals.add(AccessPrincipal.publicAccess());
        principals.add(AccessPrincipal.player(player.getUniqueId()));
        boolean manager = false;
        if (profile.isOwner(player.getUniqueId())) {
            principals.add(new AccessPrincipal(AccessPrincipalType.PROPERTY_OWNER, player.getUniqueId().toString()));
            manager = true;
        }
        AccessProviderRegistry.SubjectSnapshot provided =
                providerRegistry.snapshot(profile, player.getUniqueId(), player.getName());
        principals.addAll(provided.principals());
        manager |= provided.manager();
        return new Subject(Set.copyOf(principals), manager);
    }

    Set<AccessPrincipal> principalsFor(AccessProfile profile, Player player) {
        return subjectFor(profile, player).principals();
    }

    private boolean hasAllowedManageRule(AccessProfile profile, Set<AccessPrincipal> principals) {
        if (profile == null || principals == null || principals.isEmpty()) {
            return false;
        }
        return regionRulesByProfile.getOrDefault(profile.getId(), List.of()).stream()
                .anyMatch(rule -> rule != null
                        && rule.isAllow()
                        && rule.matches(AccessAction.MANAGE, principals));
    }

    private void saveBlockPresetRules(AccessProfile profile, String world, int x, int y, int z, AccessPreset preset) {
        List<AccessRule> rules = rulesForBlockPreset(profile, world, x, y, z, preset);
        for (AccessRule rule : rules) {
            storage.saveRule(rule);
        }
    }

    private List<AccessRule> rulesForBlockPreset(AccessProfile profile, String world, int x, int y, int z, AccessPreset preset) {
        List<AccessRule> rules = new ArrayList<>();
        rules.add(new AccessRule(null, profile.getId(), AccessRuleScope.BLOCK, world, x, y, z,
                AccessPrincipal.marker(), EnumSet.copyOf(AccessAction.ALL_ACTIONS), true, Instant.now()));
        Set<AccessAction> useActions = EnumSet.copyOf(AccessAction.USE_ACTIONS);
        switch (preset) {
            case PUBLIC -> rules.add(new AccessRule(null, profile.getId(), AccessRuleScope.BLOCK, world, x, y, z,
                    AccessPrincipal.publicAccess(), useActions, true, Instant.now()));
            case MEMBERS -> {
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.PROPERTY_MEMBER, useActions));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_MEMBER, useActions));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_MANAGER, useActions));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_DIRECTOR, useActions));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_OWNER, useActions));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.HOTEL_GUEST, useActions));
            }
            case MANAGERS -> {
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.PROPERTY_OWNER, AccessAction.ALL_ACTIONS));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_MANAGER, AccessAction.ALL_ACTIONS));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_DIRECTOR, AccessAction.ALL_ACTIONS));
                rules.add(blockPrincipal(profile, world, x, y, z, AccessPrincipalType.COMPANY_OWNER, AccessAction.ALL_ACTIONS));
            }
            case PRIVATE, CUSTOM -> {
            }
        }
        return rules;
    }

    private AccessRule blockPrincipal(AccessProfile profile, String world, int x, int y, int z,
                                      AccessPrincipalType type, Set<AccessAction> actions) {
        return new AccessRule(null, profile.getId(), AccessRuleScope.BLOCK, world, x, y, z,
                new AccessPrincipal(type, "*"), actions, true, Instant.now());
    }

    private OwnerIdentity resolveOwner(AccessProfileType type, String ownerArg) {
        String raw = ownerArg == null ? "" : ownerArg.trim();
        if (type == AccessProfileType.COMPANY || type == AccessProfileType.HOTEL_ROOM) {
            return new OwnerIdentity(null, null, raw);
        }
        UUID uuid = null;
        String name = raw;
        try {
            uuid = raw.isBlank() ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact(raw);
            if (online != null) {
                uuid = online.getUniqueId();
                name = online.getName();
            }
        }
        return new OwnerIdentity(uuid, name, raw.isBlank() ? null : raw);
    }

    private String displayName(AccessProfileType type, String regionId, OwnerIdentity owner) {
        if (owner.ownerKey() != null && !owner.ownerKey().isBlank()) {
            return type.name() + " " + owner.ownerKey();
        }
        return type.name() + " " + regionId;
    }

    private <T> Map<String, List<T>> freezeListMap(Map<String, List<T>> source) {
        Map<String, List<T>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }

    private <T> Map<String, Map<String, List<T>>> freezeNestedListMap(Map<String, Map<String, List<T>>> source) {
        Map<String, Map<String, List<T>>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<T>>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), freezeListMap(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }

    private CompletableFuture<Void> runStorage(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    private <T> CompletableFuture<T> supplyStorage(java.util.concurrent.Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }, executor);
    }

    private String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        return cause != null && cause.getMessage() != null ? cause.getMessage()
                : cause == null ? "Errore sconosciuto" : cause.getClass().getSimpleName();
    }

    record Subject(Set<AccessPrincipal> principals, boolean manager) {
    }

    private record OwnerIdentity(UUID ownerUuid, String ownerName, String ownerKey) {
    }
}
