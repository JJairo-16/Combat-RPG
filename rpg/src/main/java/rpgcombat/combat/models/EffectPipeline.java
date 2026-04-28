package rpgcombat.combat.models;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.characters.Character;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Gestiona l'execució de fases d'efectes.
 */
public class EffectPipeline {

    /**
     * Executa una fase amb atacant, defensor i arma.
     */
    public void runPhase(
            HitContext ctx,
            Phase phase,
            Character attacker,
            Character defender,
            Weapon weapon,
            Random attackerRng,
            Random defenderRng,
            CombatMessageBuffer out) {

        attacker.triggerEffects(ctx, phase, attackerRng, out);
        defender.triggerEffects(ctx, phase, defenderRng, out);

        if (weapon != null) {
            weapon.triggerPhase(ctx, attackerRng, phase, out);
        }
    }

    /**
     * Executa una fase només amb l'atacant.
     */
    public void runAttackerOnly(
            HitContext ctx,
            Phase phase,
            Character attacker,
            Random attackerRng,
            CombatMessageBuffer out) {

        attacker.triggerEffects(ctx, phase, attackerRng, out);
    }
}
