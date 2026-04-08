package rpgcombat.combat;

import java.util.List;
import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.weapons.Weapon;
import rpgcombat.models.weapons.passives.HitContext;
import rpgcombat.models.weapons.passives.HitContext.Phase;

public class EffectPipeline {

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

    public void runAttackerOnly(
            HitContext ctx,
            Phase phase,
            Character attacker,
            Random attackerRng,
            List<String> out) {

        attacker.triggerEffects(ctx, phase, attackerRng, out);
    }
}