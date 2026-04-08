package rpgcombat.combat;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.models.characters.Statistics;

public class RoundRecoveryService {

    public void registerDefenseBonus(
            Action defenderAction,
            Result defenderResult,
            double incomingDamage,
            EndRoundRegenBonus bonus) {

        if (bonus == null || incomingDamage <= 0) {
            return;
        }

        double damageReceived = defenderResult.recived();

        if (defenderAction == Action.DODGE && wasSuccessfulDodge(damageReceived, incomingDamage)) {
            bonus.add(0.005, 0.01);
            return;
        }

        if (defenderAction == Action.DEFEND && wasSuccessfulDefense(damageReceived, incomingDamage)) {
            bonus.add(0.004, 0.01);
        }
    }

    public void applyEndRoundBonus(Character character, EndRoundRegenBonus bonus) {
        if (character == null || bonus == null) {
            return;
        }

        Statistics stats = character.getStatistics();

        double hpAmount = stats.getMaxHealth() * bonus.bonusHealthPct();
        double manaAmount = stats.getMaxMana() * bonus.bonusManaPct();

        if (hpAmount > 0) {
            stats.heal(hpAmount);
        }

        if (manaAmount > 0) {
            stats.restoreMana(manaAmount);
        }
    }

    private boolean wasSuccessfulDodge(double damageReceived, double incomingDamage) {
        return incomingDamage > 0 && damageReceived == 0;
    }

    private boolean wasSuccessfulDefense(double damageReceived, double incomingDamage) {
        return incomingDamage > 0 && damageReceived < incomingDamage;
    }
}