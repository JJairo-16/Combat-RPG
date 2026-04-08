package rpgcombat.combat;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.models.weapons.AttackResult;
import rpgcombat.models.weapons.Target;

public class AttackResolver {

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

    public Character chooseTarget(Character attacker, Character defender, AttackResult attackResult) {
        Target target = attackResult.target();
        if (target == null || target == Target.ENEMY) {
            return defender;
        }
        return attacker;
    }
}