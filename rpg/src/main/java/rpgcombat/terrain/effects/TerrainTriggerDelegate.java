package rpgcombat.terrain.effects;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Interfície comuna per delegar un terreny a un trigger personalitzat.
 */
interface TerrainTriggerDelegate {
    /**
     * Executa el trigger a la fase activa.
     *
     * @param ctx context del cop
     * @param phase fase del pipeline
     * @param rng aleatorietat de la fase
     * @param owner combatent que conté l'efecte de terreny
     * @return resultat produït pel trigger
     */
    EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner);

    /**
     * Propaga l'inici de ronda al trigger delegat.
     *
     * @param owner combatent propietari
     * @param roundNumber ronda actual
     * @param rng aleatorietat de ronda
     * @param out sortida de missatges
     */
    default void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
    }

    /**
     * Propaga el tancament de ronda al trigger delegat.
     *
     * @param owner combatent propietari
     */
    default void onRoundEnd(Character owner) {
    }

    /**
     * Consulta si el trigger delegat anul·la la regeneració passiva de vida.
     *
     * @param owner combatent propietari
     * @return {@code true} quan s'ha d'ometre la regeneració passiva
     */
    default boolean suppressPassiveHealthRegen(Character owner) {
        return false;
    }

    /**
     * Consulta una pista curta que el menú d'accions pot mostrar abans de triar.
     *
     * @param owner combatent propietari
     * @param nextRound ronda que començarà després de triar accions
     * @return pista visible o text buit quan el trigger no en necessita
     */
    default String actionMenuHint(Character owner, int nextRound) {
        return "";
    }
}
