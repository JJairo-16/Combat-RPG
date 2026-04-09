package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.weapons.passives.HitContext;

public abstract class ConstantDamageEffect implements Effect {
    protected static String key = "ConstantDamageEffect";

    protected final EffectState state;
    protected double damagePerTurn;

    protected ConstantDamageEffect(int turns, double damagePerTurn) {
        this.state = EffectState.ofDuration(turns);
        this.damagePerTurn = damagePerTurn;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        if (isExpired()) {
            return EffectResult.none();
        }

        state.tickDuration();

        return EffectResult.none();
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }
}
