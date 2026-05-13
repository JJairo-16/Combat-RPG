package rpgcombat.game.modifier.ultimate;

import java.util.Random;

import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.types.MenuTurnEffect;
import rpgcombat.weapons.passives.HitContext;

/** Boost d'un sol cop creat quan s'activa una ulti de segona etapa. */
public final class UltimateChargeBoost implements Effect, MenuTurnEffect {
    public static final String INTERNAL_EFFECT_KEY = "ULTIMATE_CHARGE_BOOST";
    private static final double COLOSSAL_STAGGER_CHANCE = 0.35;

    private final UltimateActionType type;
    private final EffectState state = new EffectState(1, 1, 1, 0);
    private boolean applied;

    public UltimateChargeBoost(UltimateActionType type) {
        this.type = type;
    }

    public UltimateActionType type() {
        return type;
    }

    @Override
    public String key() {
        return INTERNAL_EFFECT_KEY;
    }

    @Override
    public int priority() {
        return 250;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }

    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (type == UltimateActionType.ELVEN_OPENING_SHOT && !applied) {
            ctx.setCriticalChance(ctx.criticalChance() + type.critChanceBonus());
            ctx.putMeta("ultimateCritBonus", type.critChanceBonus());
            return EffectResult.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                    "El tret èlfic troba una obertura gairebé impossible.");
        }
        return EffectResult.none();
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (applied || state.charges() <= 0) {
            return EffectResult.none();
        }

        applied = true;
        state.consumeCharge();
        ctx.multiplyDamage(type.damageMultiplier());
        ctx.putMeta("ultimateUsed", true);
        ctx.putMeta("ultimateId", type.discoveryId());
        ctx.putMeta("ultimateName", type.label());
        ctx.putMeta("ultimateType", type.weaponType().name());
        ctx.putMeta("ultimateMultiplier", type.damageMultiplier());
        ctx.putMeta("secondStageCharge", true);

        return EffectResult.styled(MessageColor.MAGENTA, MessageSymbol.POSITIVE,
                type.label() + " converteix la càrrega en una ruptura decisiva.");
    }

    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        if (!applied || type != UltimateActionType.COLOSSAL_BREAK || ctx.damageDealt() <= 0) {
            return EffectResult.none();
        }
        if (rng.nextDouble() < COLOSSAL_STAGGER_CHANCE && ctx.defender() != null) {
            ctx.defender().applyStagger(1);
            ctx.putMeta("ultimateStaggerApplied", true);
            return EffectResult.styled(MessageColor.YELLOW, MessageSymbol.POSITIVE,
                    "El trencament colossal desequilibra el rival.");
        }
        ctx.putMeta("ultimateStaggerApplied", false);
        return EffectResult.none();
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        state.setDuration(0);
        return EffectResult.none();
    }

    @Override
    public void onMenuTurnEnd(Character owner) {
        state.setDuration(0);
    }

    public static boolean isArmed(Character owner) {
        if (owner == null) {
            return false;
        }
        for (Effect effect : owner.getEffects()) {
            if (effect instanceof UltimateChargeBoost boost && !boost.isExpired()) {
                return true;
            }
        }
        return false;
    }
}
