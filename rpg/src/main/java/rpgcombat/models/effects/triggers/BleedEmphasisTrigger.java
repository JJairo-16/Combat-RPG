package rpgcombat.models.effects.triggers;

import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/** Trigger de mode que fa que el sagnat pesi més dins del combat. */
public final class BleedEmphasisTrigger extends Trigger {
    public static final String INTERNAL_EFFECT_KEY = "BLEED_EMPHASIS";
    public static final double DEFAULT_DAMAGE_MULTIPLIER = 1.12;
    public static final int DEFAULT_CRITICAL_BLEED_TURNS = 3;
    public static final int DEFAULT_DEEP_CUT_BLEED_TURNS = 2;

    private final double damageMultiplier;
    private final int criticalBleedTurns;
    private final int deepCutBleedTurns;

    public BleedEmphasisTrigger() {
        this(DEFAULT_DAMAGE_MULTIPLIER, DEFAULT_CRITICAL_BLEED_TURNS, DEFAULT_DEEP_CUT_BLEED_TURNS);
    }

    public BleedEmphasisTrigger(double damageMultiplier, int criticalBleedTurns, int deepCutBleedTurns) {
        super(INTERNAL_EFFECT_KEY);
        this.damageMultiplier = Math.max(1.0, damageMultiplier);
        this.criticalBleedTurns = Math.max(1, criticalBleedTurns);
        this.deepCutBleedTurns = Math.max(1, deepCutBleedTurns);
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (!isOwnerAttacker(ctx, owner) || !ctx.defender().isBleeding() || damageMultiplier <= 1.0) {
            return EffectResult.none();
        }

        ctx.multiplyDamage(damageMultiplier);
        ctx.putMeta("modeBleedEmphasis", true);
        ctx.putMeta("modeBleedEmphasisDamageMultiplier", damageMultiplier);

        return EffectResult.gamemode(
                MessageColor.RED,
                MessageSymbol.WARNING,
                "El sagnat obert amplifica el cop.");
    }

    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        if (!isOwnerAttacker(ctx, owner) || ctx.damageDealt() <= 0) {
            return EffectResult.none();
        }

        if (ctx.wasCritical()) {
            ctx.defender().applyBleed(criticalBleedTurns);
            ctx.putMeta("modeBleedEmphasisCriticalBleed", criticalBleedTurns);
            return EffectResult.gamemode(
                    MessageColor.RED,
                    MessageSymbol.POSITIVE,
                    "La ferida crítica sagna més temps.");
        }

        Object rawDamageInput = ctx.getMeta("RAW_DAMAGE");
        if (rawDamageInput instanceof Number rawDamage
                && ctx.defenderAction() == Action.DODGE
                && ctx.damageDealt() >= rawDamage.doubleValue() * 0.90) {
            ctx.defender().applyBleed(deepCutBleedTurns);
            ctx.putMeta("modeBleedEmphasisDeepCutBleed", deepCutBleedTurns);
            return EffectResult.gamemode(
                    MessageColor.RED,
                    MessageSymbol.POSITIVE,
                    "El tall superficial s'obre més del normal.");
        }

        return EffectResult.none();
    }

    private static boolean isOwnerAttacker(HitContext ctx, Character owner) {
        return ctx != null && owner != null && owner == ctx.attacker() && ctx.defender() != null;
    }
}
