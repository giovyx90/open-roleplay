package dev.openrp.crime.config;

/**
 * A configured racket escalation step. The highest configured effect, {@code none}, is a deliberate
 * narrative signal: "from here on it is unassisted RP". The plugin never destroys a company.
 *
 * @param level  1..3
 * @param name   shown to players
 * @param effect one of {@code notification_to_owner}, {@code company_reputation_malus}, {@code none}
 * @param reputationMalus malus applied when {@code effect = company_reputation_malus}
 */
public record EscalationLevel(int level, String name, String effect, int reputationMalus) {

    public EscalationLevel {
        name = name == null ? ("Livello " + level) : name;
        effect = effect == null ? "none" : effect;
        reputationMalus = Math.max(0, reputationMalus);
    }
}
