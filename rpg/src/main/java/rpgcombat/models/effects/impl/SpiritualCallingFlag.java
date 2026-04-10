package rpgcombat.models.effects.impl;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.MenuTurnEffect;
import rpgcombat.utils.rng.SpiritualCallingDie;

public class SpiritualCallingFlag implements Effect, MenuTurnEffect {
    public static final String GLOBAL_EFFECT_KEY = "CAN_CALL_SPIRITS";
    public static final int COOLDOWN_TURNS = 3;

    private final EffectState state = new EffectState(0, 1, 0, 0);

    @Override
    public String key() {
        return GLOBAL_EFFECT_KEY;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public boolean canUse() {
        return !state.onCooldown();
    }

    public int cooldownTurns() {
        return state.cooldownTurns();
    }

    public double invoke(Character owner, Random rng) {
        if (!canUse()) {
            return 0;
        }

        Statistics stats = owner.getStatistics();
        double healPercent = SpiritualCallingDie.roll(rng, owner.getStatistics());
        double healAmount = stats.getMaxHealth() * healPercent;
        double healed = stats.heal(healAmount);

        state.setCooldown(COOLDOWN_TURNS);
        return healed;
    }

    @Override
    public void onMenuTurnEnd(Character owner) {
        state.tickCooldown();
    }
}
