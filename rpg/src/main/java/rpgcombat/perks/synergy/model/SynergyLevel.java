package rpgcombat.perks.synergy.model;

import java.util.List;

import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Defineix un nivell de sinergia segons el nombre de membres. */
public record SynergyLevel(
        int members,
        Phase trigger,
        List<Rule> conditions,
        List<Rule> actions) {

    /**
     * Normalitza condicions i accions evitant valors nuls.
     */
    public SynergyLevel {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
