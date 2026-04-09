package rpgcombat.combat;

import java.util.List;
import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Gestiona l'execució de les fases d'efectes durant el combat.
 */
public class EffectPipeline {

    /**
     * Executa una fase aplicant efectes de l'atacant, defensor i arma (si n'hi ha).
     *
     * @param ctx Context de l'impacte
     * @param phase Fase actual
     * @param attacker Personatge atacant
     * @param defender Personatge defensor
     * @param weapon Arma utilitzada (pot ser null)
     * @param attackerRng Generador aleatori de l'atacant
     * @param defenderRng Generador aleatori del defensor
     * @param out Llista de sortida de missatges
     */
    public void runPhase(
            HitContext ctx,
            Phase phase,
            Character attacker,
            Character defender,
            Weapon weapon,
            Random attackerRng,
            Random defenderRng,
            List<String> out) {

        attacker.triggerEffects(ctx, phase, attackerRng, out);
        defender.triggerEffects(ctx, phase, defenderRng, out);

        if (weapon != null) {
            weapon.triggerPhase(ctx, attackerRng, phase, out);
        }
    }

    /**
     * Executa una fase només amb els efectes de l'atacant.
     *
     * @param ctx Context de l'impacte
     * @param phase Fase actual
     * @param attacker Personatge atacant
     * @param attackerRng Generador aleatori de l'atacant
     * @param out Llista de sortida de missatges
     */
    public void runAttackerOnly(
            HitContext ctx,
            Phase phase,
            Character attacker,
            Random attackerRng,
            List<String> out) {

        attacker.triggerEffects(ctx, phase, attackerRng, out);
    }
}