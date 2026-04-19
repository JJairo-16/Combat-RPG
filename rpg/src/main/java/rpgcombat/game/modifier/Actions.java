package rpgcombat.game.modifier;

import menu.model.MenuResult;
import rpgcombat.combat.Action;
import rpgcombat.game.modifier.ui.Messages.CALL_SPIRITS;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.rng.SpiritualCallingDie;
import rpgcombat.utils.rng.SpiritualCallingDie.RollResult;
import rpgcombat.utils.ui.Cleaner;

public final class Actions {
    private static final Cleaner cleaner = new Cleaner();

    private static final int SPIRITUAL_CALLING_COOLDOWN = 3;

    private Actions() {
    }

    public static MenuResult<Action> spiritualCalling(Character player) {
        cleaner.clear();
        
        if (!player.hasEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY)) {
            cannotUseSpiritualCalling();
            return MenuResult.repeatLoop();
        }
        
        SpiritualCallingFlag effect = (SpiritualCallingFlag) player.getEffect(SpiritualCallingFlag.INTERNAL_EFFECT_KEY);
        if (!effect.canActivate()) {
            cannotUseSpiritualCalling();
            return MenuResult.repeatLoop();
        }
        
        effect.use();

        CALL_SPIRITS.CALL_INIT.print();

        int charisma = player.getStatistics().getCharisma();
        System.out.println(DivineCharismaAffinity.classifyStanding(charisma).toString());

        System.out.println();
        Menu.pause();
        System.out.println();

        RollResult result = SpiritualCallingDie.roll(
                player.rng(),
                player.getStatistics());

        int face = result.face();
        double percentage = result.percent();

        double maxHp = player.getStatistics().getMaxHealth();
        double healAmount = maxHp * percentage;

        player.getStatistics().heal(healAmount);
        player.setSpiritualCallingCooldown(SPIRITUAL_CALLING_COOLDOWN);

        System.out.println();
        CALL_SPIRITS.classifyShot(face).print();

        Menu.pause();

        return MenuResult.repeatLoop();
    }

    private static void cannotUseSpiritualCalling() {
        CALL_SPIRITS.CALL_IN_COOLDOWN.print();
        Menu.pause();
    }
}