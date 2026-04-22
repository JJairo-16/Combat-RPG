package rpgcombat.combat.services;

import java.util.List;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.Exhaustion;
import rpgcombat.models.effects.impl.Fatigue;
import rpgcombat.utils.ui.Ansi;

/**
 * Gestiona els recursos ocults de ritme de combat.
 *
 * <p>Stamina afavoreix alternar entre atacar i accions no ofensives.
 * Resistencia afavoreix alternar entre atacar i respondre defensivament.
 * El sistema penalitza lleugerament l'spam d'una mateixa opció, però sense
 * col·lapsar el personatge massa aviat.</p>
 *
 * <p>La fatiga i el cansament s'apliquen al final de la resolució corresponent,
 * perquè no alterin injustament la mateixa acció que les ha provocat.</p>
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

    /**
     * Aplica el desgast defensiu només quan realment hi ha un atac entrant.
     * Resistencia baixa només en DEFEND/DODGE.
     */
    public void onDefenseReaction(Character defender, Action defenseAction, double incomingDamage) {
        if (defender == null || defenseAction == null || incomingDamage <= 0) {
            return;
        }

        Statistics stats = defender.getStatistics();
        switch (defenseAction) {
            case DEFEND -> stats.consumeResistanceOnDefend();
            case DODGE -> stats.consumeResistanceOnDodge();
            default -> {
                // La resistencia no baixa fora de DEFEND/DODGE.
            }
        }
    }

    /**
     * Aplica fatiga després de completar l'acció ofensiva.
     * Així no penalitza el mateix cop que l'ha desencadenat.
     */
    public void onAttackResolved(Character actor, Action action, double attemptedDamage, List<String> out) {
        if (actor == null || action != Action.ATTACK || attemptedDamage <= 0) {
            return;
        }
        maybeApplyFatigue(actor, out);
    }

    /**
     * Aplica cansament defensiu després d'haver resolt la defensa.
     * Així no empitjora la mateixa DEFEND/DODGE que l'ha generat.
     */
    public void onDefenseResolved(Character defender, Action defenseAction, double incomingDamage, List<String> out) {
        if (defender == null || defenseAction == null || incomingDamage <= 0) {
            return;
        }
        switch (defenseAction) {
            case DEFEND, DODGE -> maybeApplyExhaustion(defender, out);
            default -> {
                // No s'aplica fora de DEFEND/DODGE.
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

    /** Multiplica el dany rebut segons la resistencia actual del defensor. */
    public void applyDefensivePressure(Character defender, HitModifier target) {
        if (defender == null || target == null) {
            return;
        }

        target.multiply(defender.getStatistics().resistanceIncomingDamageMultiplier());
    }

    private void maybeApplyFatigue(Character actor, List<String> out) {
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
            out.add("[YELLOW|!]La cadena ofensiva pesa massa: apareix la fatiga.");
        }
    }

    private void maybeApplyExhaustion(Character defender, List<String> out) {
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
            out.add(Ansi.YELLOW + "  ! La pressió defensiva et supera: apareix el cansament.");
        }
    }

    @FunctionalInterface
    public interface HitModifier {
        void multiply(double multiplier);
    }
}
