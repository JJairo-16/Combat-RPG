package rpgcombat.combat;

import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.attack.Target;

/**
 * Gestiona la resolució d’atacs i la selecció d’objectius.
 */
public class AttackResolver {

    /**
     * Resol l’efecte d’un atac segons l’acció defensiva.
     *
     * @return resultat aplicat al defensor
     */
    public Result resolveAttack(double damage, Character target, Action targetAction) {
        return switch (targetAction) {
            case DODGE -> target.dodge(damage);
            case DEFEND -> target.defend(damage);
            default -> {
                if (damage <= 0) {
                    yield new Result(-1, "");
                }
                yield target.getDamage(damage);
            }
        };
    }

   /** Determina qui rep l’atac (enemic o propi atacant). */
    public Character chooseTarget(Character attacker, Character defender, AttackResult attackResult) {
        Target target = attackResult.target();
        if (target == null || target == Target.ENEMY) {
            return defender;
        }
        return attacker;
    }
}