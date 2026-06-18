package dev.openrp.politics.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.politics.OpenPoliticsPlugin;
import dev.openrp.politics.adapter.AdapterRegistry;
import dev.openrp.politics.capability.PoliticalCapability;
import dev.openrp.politics.config.ChargeDef;
import dev.openrp.politics.config.Government;
import dev.openrp.politics.core.ActService;
import dev.openrp.politics.core.ChargeManager;
import dev.openrp.politics.core.ElectionService;
import dev.openrp.politics.core.GovernmentManager;
import dev.openrp.politics.core.LawService;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/** Thin delegating implementation of {@link OpenPoliticsApi} backed by the plugin's live services. */
public final class OpenPoliticsApiProvider implements OpenPoliticsApi {

    private final OpenPoliticsPlugin plugin;

    public OpenPoliticsApiProvider(OpenPoliticsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }

    @Override
    public List<Government> governments() {
        return plugin.governments().all();
    }

    @Override
    public boolean isGovernmentActive(String governmentId) {
        return plugin.governments().isActive(governmentId);
    }

    @Override
    public List<ChargeDef> charges() {
        return List.copyOf(plugin.config().charges().all());
    }

    @Override
    public Optional<ChargeDef> getCharge(String chargeId) {
        return plugin.config().charges().get(chargeId);
    }

    @Override
    public List<ChargeHolder> holdersOf(String chargeId) {
        return plugin.charges().activeHoldersOf(chargeId);
    }

    @Override
    public boolean isVacant(String chargeId) {
        return plugin.charges().isVacant(chargeId);
    }

    @Override
    public List<ChargeDef> chargesOf(UUID player) {
        return plugin.charges().chargesOf(player);
    }

    @Override
    public boolean hasCapability(UUID player, PoliticalCapability capability) {
        return plugin.charges().has(player, capability);
    }

    @Override
    public List<String> chargesWithCapability(String governmentId, PoliticalCapability capability) {
        return plugin.charges().chargesWithCapability(governmentId, capability);
    }

    @Override
    public Optional<PoliticalAct> getAct(String actId) {
        return plugin.acts().get(actId);
    }

    @Override
    public List<PoliticalAct> recentActs(int limit) {
        return plugin.acts().recent(limit);
    }

    @Override
    public List<Law> getActiveLaws(String governmentId) {
        return plugin.laws().active(governmentId);
    }

    @Override
    public Optional<Law> getLaw(String lawId) {
        return plugin.laws().get(lawId);
    }

    @Override
    public boolean wasActiveDuring(String lawId, Instant moment) {
        return moment != null && plugin.laws().wasActiveDuring(lawId, moment.toEpochMilli());
    }

    @Override
    public List<Election> openElections() {
        return plugin.elections().open();
    }

    @Override
    public Optional<Election> getElection(String electionId) {
        return plugin.elections().get(electionId);
    }

    @Override
    public GovernmentManager governmentManager() {
        return plugin.governments();
    }

    @Override
    public ChargeManager chargeManager() {
        return plugin.charges();
    }

    @Override
    public ActService actService() {
        return plugin.acts();
    }

    @Override
    public LawService lawService() {
        return plugin.laws();
    }

    @Override
    public ElectionService electionService() {
        return plugin.elections();
    }
}
