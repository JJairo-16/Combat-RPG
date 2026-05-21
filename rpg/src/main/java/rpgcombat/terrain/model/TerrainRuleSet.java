package rpgcombat.terrain.model;

import java.util.List;

import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Regla completa del motor genèric de terrenys.
 *
 * @param trigger fase on s'avalua la regla
 * @param conditions condicions que han de complir-se abans d'actuar
 * @param actions accions executades quan la regla coincideix
 */
public record TerrainRuleSet(
        Phase trigger,
        List<TerrainRule> conditions,
        List<TerrainRule> actions) {

    /**
     * Aplica valors per defecte i converteix les col·leccions en immutables.
     */
    public TerrainRuleSet {
        trigger = trigger == null ? Phase.MODIFY_DAMAGE : trigger;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
