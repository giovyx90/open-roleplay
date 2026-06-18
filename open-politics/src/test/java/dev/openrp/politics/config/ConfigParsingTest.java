package dev.openrp.politics.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.politics.capability.PoliticalCapability;

/** Parses config snippets headlessly (YamlConfiguration needs no running server). */
public class ConfigParsingTest {

    private YamlConfiguration yaml(String content) {
        return YamlConfiguration.loadConfiguration(new StringReader(content));
    }

    @Test
    public void chargeInheritsGovernmentDefaultMechanismAndParsesCapabilities() {
        GovernmentCatalog governments = new GovernmentCatalog();
        governments.load(yaml("""
                governments:
                  comune:
                    display_name: "Comune"
                    sigla: "COM"
                    active: true
                    assignment_mechanism: election
                    charges: [sindaco, consiglio]
                """).getConfigurationSection("governments"));

        ChargeCatalog charges = new ChargeCatalog();
        charges.load(yaml("""
                charges:
                  sindaco:
                    display_name: "Sindaco"
                    government_id: comune
                    authority_level: 10
                    max_holders: 1
                    term_duration_days: 30
                    capabilities: [SIGN_ACT, APPOINT, BOGUS_CAP]
                  consiglio:
                    display_name: "Consiglio"
                    government_id: comune
                    max_holders: 8
                    capabilities: [SIGN_ACT]
                    collegiate:
                      enabled: true
                      quorum: 0.6
                      majority: 0.5
                      duration_hours: 48
                """).getConfigurationSection("charges"), governments);

        ChargeDef sindaco = charges.get("sindaco").orElseThrow();
        assertEquals("Sindaco", sindaco.displayName());
        assertEquals("election", sindaco.mechanism().type());
        assertTrue(sindaco.grants(PoliticalCapability.APPOINT));
        assertFalse("an unknown capability is skipped, not fatal", sindaco.grants(PoliticalCapability.VETO));

        ChargeDef consiglio = charges.get("consiglio").orElseThrow();
        assertTrue("max_holders > 1 makes it collegiate", consiglio.isCollegiate());
        assertTrue(consiglio.collegiate().enabled());
        assertEquals(0.6, consiglio.collegiate().quorum(), 0.0001);
    }

    @Test
    public void actTypeParsesIterFields() {
        ActTypeCatalog catalog = new ActTypeCatalog();
        catalog.load(yaml("""
                act_types:
                  proposta_legge:
                    display_name: "Proposta di Legge"
                    capability_required: SIGN_ACT
                    can_become_law: true
                    requires_vote: true
                    submit_to: consiglio
                    veto_allowed: true
                    veto_window_hours: 24
                    law_category: civile
                """).getConfigurationSection("act_types"));

        ActType type = catalog.get("proposta_legge").orElseThrow();
        assertTrue(type.canBecomeLaw());
        assertTrue(type.requiresVote());
        assertEquals("consiglio", type.submitTo());
        assertEquals("civile", type.lawCategory());
        assertEquals(PoliticalCapability.SIGN_ACT, type.required());
    }
}
