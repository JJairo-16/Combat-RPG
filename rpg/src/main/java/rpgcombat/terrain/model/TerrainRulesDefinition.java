package rpgcombat.terrain.model;

import java.util.List;

import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Configuració del motor genèric d'un terreny.
 *
 * @param entries regles completes declarades pel terreny
 * @param trigger fase abreujada usada quan el JSON declara una sola regla
 * @param conditions condicions abreujades de la regla única
 * @param actions accions abreujades de la regla única
 */
public record TerrainRulesDefinition(
        List<TerrainRuleSet> entries,
        Phase trigger,
        List<TerrainRule> conditions,
        List<TerrainRule> actions) {

    /**
     * Expandeix la forma abreujada i protegeix les llistes de configuració.
     */
    public TerrainRulesDefinition {
        if (entries == null || entries.isEmpty()) {
            entries = List.of(new TerrainRuleSet(trigger, conditions, actions));
        } else {
            entries = List.copyOf(entries);
        }
        trigger = trigger == null ? Phase.MODIFY_DAMAGE : trigger;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
