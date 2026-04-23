package rpgcombat.combat;

import rpgcombat.combat.models.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.attack.Target;

/**
 * Resol atacs i determina l'objectiu.
 */
public class AttackResolver {

    /**
     * Resol el resultat d'un atac segons l'acció del defensor.
     *
     * @param damage dany a aplicar
     * @param target objectiu de l'atac
     * @param targetAction acció defensiva
     * @return resultat de l'impacte
     */
    public Result resolveAttack(double damage, Character target, Action targetAction) {
        return switch (targetAction) {
            case DODGE -> target.dodge(damage);
            case DEFEND -> target.defend(damage);
            default -> {
                if (damage <= 0) {
                    yield new Result(-1, "");
                }
                target.resetGuardStacks();
                yield target.getDamage(damage);
            }
        };
    }

    /**
     * Determina qui rep l'atac.
     *
     * @param attacker atacant
     * @param defender defensor
     * @param attackResult resultat de l'atac
     * @return personatge objectiu
     */
    public Character chooseTarget(Character attacker, Character defender, AttackResult attackResult) {
        Target target = attackResult.target();
        if (target == null || target == Target.ENEMY) return defender;
        return attacker;
    }
}