package rpgcombat.models.effects.impl;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.MenuTurnEffect;

public class SpiritualCallingFlag implements Effect, MenuTurnEffect {
    public static final String INTERNAL_EFFECT_KEY = "CAN_CALL_SPIRITS";
    public static final int COOLDOWN_TURNS = 3;
    private boolean canBeUsed = true;

    private final EffectState state = new EffectState(0, 1, 0, 0);

    @Override
    public String key() {
        return INTERNAL_EFFECT_KEY;
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

    @Override
    public void onMenuTurnEnd(Character owner) {
        state.tickCooldown();
    }

    public boolean canActivate() {
        return canBeUsed;
    }

    public void use() {
        canBeUsed = false;
        state.setCooldown(COOLDOWN_TURNS);
    }
}
