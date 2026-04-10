package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Efecte que pot fer fallar els atacs del portador.
 */
public abstract class MissChanceEffect extends TimedEffect {

    protected MissChanceEffect(String key, int turns) {
        super(key, turns);
    }

    protected abstract boolean applies(HitContext ctx, Character owner);

    protected abstract double missChance(HitContext ctx, Random rng, Character owner);

    protected String buildMessage(Character owner) {
        return owner.getName() + " falla l'atac.";
    }

    @Override
    public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
        if (isExpired() || !applies(ctx, owner)) {
            return EffectResult.none();
        }

        double chance = Math.clamp(missChance(ctx, rng, owner), 0.0, 1.0);

        if (rng.nextDouble() >= chance) {
            return EffectResult.none();
        }

        ctx.setBaseDamage(0);
        return EffectResult.msg(buildMessage(owner));
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        state.tickDuration();
        return EffectResult.none();
    }
}