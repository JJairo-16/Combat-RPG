package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
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

    protected CombatMessage buildMessage(Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.RED,
                owner.getName() + " falla l'atac."
        );
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
        ctx.markEffectFail(key());
        return EffectResult.msg(buildMessage(owner));
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        state.tickDuration();
        return EffectResult.none();
    }
}