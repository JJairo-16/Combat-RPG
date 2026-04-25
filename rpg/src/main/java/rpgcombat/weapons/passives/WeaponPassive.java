package rpgcombat.weapons.passives;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Passiu d'arma flexible per fases.
 */
public interface WeaponPassive {

    default CombatMessage onPhase(Weapon weapon, HitContext ctx, Random rng, Phase phase) {
        return switch (phase) {
            case START_TURN -> startTurn(weapon, ctx, rng);
            case BEFORE_ATTACK -> beforeAttack(weapon, ctx, rng);
            case ROLL_CRIT -> rollCrit(weapon, ctx, rng);
            case MODIFY_DAMAGE -> modifyDamage(weapon, ctx, rng);
            case BEFORE_DEFENSE -> beforeDefense(weapon, ctx, rng);
            case AFTER_DEFENSE -> afterDefense(weapon, ctx, rng);
            case AFTER_HIT -> afterHit(weapon, ctx, rng);
            case END_TURN -> endTurn(weapon, ctx, rng);
        };
    }

    default CombatMessage startTurn(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage beforeAttack(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage rollCrit(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage beforeDefense(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage afterDefense(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) { return null; }

    default CombatMessage endTurn(Weapon weapon, HitContext ctx, Random rng) { return null; }
}