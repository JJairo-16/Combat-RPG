package rpgcombat.combat.services;

import java.util.List;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.Exhaustion;
import rpgcombat.models.effects.impl.Fatigue;

/**
 * Gestiona els recursos ocults de ritme de combat.
 */
public class CombatRhythmService {

    public void onActionStart(Character actor, Action action, List<String> out) {
        if (actor == null || action == null) return;

        actor.onTurnStart(action, out);
        if (!actor.isAlive()) return;

        Statistics stats = actor.getStatistics();
        if (action != Action.DEFEND) actor.resetGuardStacks();

        switch (action) {
            case ATTACK -> {
                stats.consumeStaminaOnAttack();
                stats.recoverResistanceOnAttack();
                maybeApplyFatigue(actor, out);
            }
            case DEFEND -> stats.recoverStaminaOnNonAttack(1.15);
            case DODGE -> stats.recoverStaminaOnNonAttack(1.05);
            case CHARGE -> stats.recoverStaminaOnNonAttack(0.90);
        }
    }

    public void onDefenseReaction(Character defender, Action defenseAction, double incomingDamage, List<String> out) {
        if (defender == null || defenseAction == null || incomingDamage <= 0) return;

        Statistics stats = defender.getStatistics();
        switch (defenseAction) {
            case DEFEND -> {
                stats.consumeResistanceOnDefend();
                maybeApplyExhaustion(defender, out);
            }
            case DODGE -> {
                stats.consumeResistanceOnDodge();
                maybeApplyExhaustion(defender, out);
            }
            default -> {
            }
        }
    }

    public void applyOffensivePressure(Character attacker, HitModifier target) {
        if (attacker == null || target == null) return;
        target.multiply(attacker.getStatistics().staminaDamageMultiplier());
        target.multiply(attacker.getAttackModifierThisTurn());
    }

    public void applyDefensivePressure(Character defender, HitModifier target) {
        if (defender == null || target == null) return;
        target.multiply(defender.getStatistics().resistanceIncomingDamageMultiplier());
        target.multiply(defender.consumeIncomingDamageMultiplier());
    }

    private void maybeApplyFatigue(Character actor, List<String> out) {
        if (actor.hasEffect(Fatigue.INTERNAL_EFFECT_KEY)) return;
        Random rng = actor.rng();
        double chance = actor.getStatistics().fatigueChance();
        if (chance <= 0 || rng.nextDouble() >= chance) return;
        actor.addEffect(new Fatigue());
        if (out != null) out.add("[YELLOW|!] La cadena ofensiva pesa massa: apareix la fatiga.");
    }

    private void maybeApplyExhaustion(Character defender, List<String> out) {
        if (defender.hasEffect(Exhaustion.INTERNAL_EFFECT_KEY)) return;
        Random rng = defender.rng();
        double chance = defender.getStatistics().exhaustionChance();
        if (chance <= 0 || rng.nextDouble() >= chance) return;
        defender.addEffect(new Exhaustion());
        if (out != null) out.add("[YELLOW|!] La pressió defensiva et supera: apareix el cansament.");
    }

    @FunctionalInterface
    public interface HitModifier {
        void multiply(double multiplier);
    }
}
