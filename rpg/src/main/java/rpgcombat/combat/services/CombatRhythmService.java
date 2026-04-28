package rpgcombat.combat.services;

import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.Exhaustion;
import rpgcombat.models.effects.impl.Fatigue;

/**
 * Gestiona els recursos ocults de ritme de combat.
 */
public class CombatRhythmService {
    /** Aplica el moviment del recurs ocult al començar el torn propi. */
    public void onActionStart(Character actor, Action action) {
        if (actor == null || action == null) {
            return;
        }

        Statistics stats = actor.getStatistics();
        if (action != Action.DEFEND) {
            actor.resetGuardStacks();
        }

        stats.onActionStart(action);
    }

    /** Aplica desgast defensiu davant un atac entrant. */
    public void onDefenseReaction(Character defender, Action defenseAction, double incomingDamage) {
        if (defender == null || defenseAction == null || incomingDamage <= 0) {
            return;
        }

        Statistics stats = defender.getStatistics();
        switch (defenseAction) {
            case DEFEND -> stats.consumeResistanceOnDefend();
            case DODGE -> stats.consumeResistanceOnDodge();
            default -> {
            }
        }
    }

    /** Aplica fatiga després de completar l'acció ofensiva. */
    public void onAttackResolved(Character actor, Action action, double attemptedDamage, CombatMessageBuffer out) {
        if (actor == null || action != Action.ATTACK || attemptedDamage <= 0) {
            return;
        }
        maybeApplyFatigue(actor, out);
    }

    /** Aplica cansament defensiu després de resoldre la defensa. */
    public void onDefenseResolved(Character defender, Action defenseAction, double incomingDamage, CombatMessageBuffer out) {
        if (defender == null || defenseAction == null || incomingDamage <= 0) {
            return;
        }
        switch (defenseAction) {
            case DEFEND, DODGE -> maybeApplyExhaustion(defender, out);
            default -> {
            }
        }
    }

    /** Multiplica el dany ofensiu segons la stamina actual. */
    public void applyOffensivePressure(Character attacker, HitModifier target) {
        if (attacker == null || target == null) {
            return;
        }
        target.multiply(attacker.getStatistics().staminaDamageMultiplier());
    }

    /** Multiplica el dany rebut segons la resistencia actual. */
    public void applyDefensivePressure(Character defender, HitModifier target) {
        if (defender == null || target == null) {
            return;
        }
        target.multiply(defender.getStatistics().resistanceIncomingDamageMultiplier());
    }

    private void maybeApplyFatigue(Character actor, CombatMessageBuffer out) {
        if (actor.hasEffect(Fatigue.INTERNAL_EFFECT_KEY)) {
            return;
        }

        Random rng = actor.rng();
        double chance = actor.getStatistics().fatigueChance();
        if (chance <= 0 || rng.nextDouble() >= chance) {
            return;
        }

        actor.addEffect(new Fatigue());
        if (out != null) {
            out.styled(MessageColor.YELLOW, MessageSymbol.WARNING,
                    "La cadena ofensiva pesa massa: apareix la fatiga.");
        }
    }

    private void maybeApplyExhaustion(Character defender, CombatMessageBuffer out) {
        if (defender.hasEffect(Exhaustion.INTERNAL_EFFECT_KEY)) {
            return;
        }

        Random rng = defender.rng();
        double chance = defender.getStatistics().exhaustionChance();
        if (chance <= 0 || rng.nextDouble() >= chance) {
            return;
        }

        defender.addEffect(new Exhaustion());
        if (out != null) {
            out.styled(MessageColor.YELLOW, MessageSymbol.WARNING,
                    "La pressió defensiva et supera: apareix el cansament.");
        }
    }

    @FunctionalInterface
    public interface HitModifier {
        void multiply(double multiplier);
    }
}
