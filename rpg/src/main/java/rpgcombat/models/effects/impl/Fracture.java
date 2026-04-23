package rpgcombat.models.effects.impl;

import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.templates.TimedEffect;
import rpgcombat.weapons.passives.HitContext;

public class Fracture extends TimedEffect {
    public static final String INTERNAL_EFFECT_KEY = "FRACTURE";
    private final double damageMultiplier = CombatBalanceRegistry.get().fracture().damageMultiplier();

    public Fracture(int turns) {
        super(INTERNAL_EFFECT_KEY, turns);
    }

    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        ctx.multiplyDamage(damageMultiplier);
        return EffectResult.msg("[RED|-]La defensa de " + owner.getName() + " s'ha vist reduida per la fractura.");
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        state().tickDuration();
        return EffectResult.none();
    }
}
