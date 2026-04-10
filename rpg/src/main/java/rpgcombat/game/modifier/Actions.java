package rpgcombat.game.modifier;

import menu.model.MenuResult;
import rpgcombat.combat.Action;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.rng.SpiritualCallingDie;

public final class Actions {

    private static final int SPIRITUAL_CALLING_COOLDOWN = 3;

    private Actions() {}

    public static MenuResult<Action> exampleKey(Character player) {
        return MenuResult.repeatLoop();
    }

    public static MenuResult<Action> spiritualCalling(Character player) {
        double percentage = SpiritualCallingDie.roll(
                player.rng(),
                player.getStatistics()
        );

        double maxHp = player.getStatistics().getMaxHealth();
        double healAmount = maxHp * percentage;

        player.getStatistics().heal(healAmount);

        player.setSpiritualCallingCooldown(SPIRITUAL_CALLING_COOLDOWN);

        return MenuResult.repeatLoop();
    }
}