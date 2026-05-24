package rpgcombat.models.effects.triggers.gamemode;

import java.util.Random;

import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.models.effects.types.EndRoundRecoveryEffect;
import rpgcombat.weapons.passives.HitContext;

/** Trigger de mode que apaga la cura passiva i converteix cada impacte en robavida. */
public final class UniversalLifeStealTrigger extends Trigger implements EndRoundRecoveryEffect {
    public static final String INTERNAL_EFFECT_KEY = "UNIVERSAL_LIFE_STEAL";
    public static final double DEFAULT_LIFE_STEAL_PCT = 0.08;
    public static final double DEFAULT_MAX_HEAL_PCT = 0.08;
    public static final boolean DEFAULT_SUPPRESS_PASSIVE_HEALTH_REGEN = true;

    private final double lifeStealPct;
    private final double maxHealPct;
    private final boolean suppressPassiveHealthRegen;

    public UniversalLifeStealTrigger() {
        this(DEFAULT_LIFE_STEAL_PCT, DEFAULT_MAX_HEAL_PCT, DEFAULT_SUPPRESS_PASSIVE_HEALTH_REGEN);
    }

    public UniversalLifeStealTrigger(double lifeStealPct, double maxHealPct, boolean suppressPassiveHealthRegen) {
        super(INTERNAL_EFFECT_KEY);
        this.lifeStealPct = Math.clamp(lifeStealPct, 0.0, 0.25);
        this.maxHealPct = Math.clamp(maxHealPct, 0.0, 0.20);
        this.suppressPassiveHealthRegen = suppressPassiveHealthRegen;
    }

    @Override
    public boolean suppressPassiveHealthRegen(Character owner) {
        return suppressPassiveHealthRegen;
    }

    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        if (!isOwnerAttacker(ctx, owner) || ctx.damageDealt() <= 0 || lifeStealPct <= 0) {
            return EffectResult.none();
        }

        ctx.putMeta("lifeStealTriggered", true);
        ctx.putMeta("lifeStealPct", lifeStealPct);
        ctx.putMeta("modeLifeSteal", true);
        double maxHeal = owner.getStatistics().getMaxHealth() * maxHealPct;
        double requestedHeal = ctx.damageDealt() * lifeStealPct;
        double realHealed = owner.getStatistics().heal(Math.min(requestedHeal, maxHeal));
        if (realHealed <= 0) {
            return EffectResult.none();
        }

        double previous = ctx.getMeta("LIFE_STOLEN", Double.class, 0.0);
        ctx.putMeta("LIFE_STOLEN", previous + realHealed);
        ctx.putMeta("modeLifeStealPct", lifeStealPct);
        ctx.putMeta("modeLifeStealHeal", realHealed);

        return EffectResult.gamemode(
                MessageColor.GREEN,
                MessageSymbol.POSITIVE,
                owner.getName() + " beu " + round1(realHealed) + " de vida de la ferida.");
    }

    private static boolean isOwnerAttacker(HitContext ctx, Character owner) {
        return ctx != null && owner != null && owner == ctx.attacker();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
