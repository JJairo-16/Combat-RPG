package rpgcombat.models.effects.triggers;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/** Trigger permanent que desestabilitza híbrids de força i intel·ligència. */
public class InternalConflict extends Trigger {
    public static final String INTERNAL_EFFECT_KEY = "INTERNAL_CONFLICT";

    private static final int SAFE_LIMIT = 16;
    private static final int HARD_LIMIT = 30;

    private static final double MIN_ACTIVE_SEVERITY = 0.25;
    private static final double FAIL_CHANCE_BASE = 0.03;
    private static final double FAIL_CHANCE_SCALE = 0.13;
    private static final double CRIT_BREAK_BASE = 0.08;
    private static final double CRIT_BREAK_SCALE = 0.24;
    private static final double DAMAGE_BREAK_BASE = 0.20;
    private static final double DAMAGE_BREAK_SCALE = 0.35;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.62;
    private static final double MAX_DAMAGE_MULTIPLIER = 0.92;

    /** Crea el trigger de conflicte intern. */
    public InternalConflict() {
        super(INTERNAL_EFFECT_KEY);
    }

    /** Indica si força i intel·ligència entren en conflicte. */
    public static boolean hasConflict(Statistics stats) {
        return conflictSeverity(stats) >= MIN_ACTIVE_SEVERITY;
    }

    /** Retorna la severitat del conflicte entre 0 i 1. */
    public static double conflictSeverity(Statistics stats) {
        return Math.clamp(rawConflictLoad(stats) / (double) (HARD_LIMIT - SAFE_LIMIT), 0.0, 1.0);
    }

    /** Pot fer fallar l'atac del portador. */
    @Override
    public EffectResult beforeAttack(HitContext ctx, Random rng, Character owner) {
        if (!isAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        double severity = activeSeverity(owner);
        if (severity < MIN_ACTIVE_SEVERITY) {
            return EffectResult.none();
        }

        double chance = FAIL_CHANCE_BASE + severity * FAIL_CHANCE_SCALE;
        if (rng.nextDouble() >= chance) {
            return EffectResult.none();
        }

        ctx.setBaseDamage(0);
        ctx.markEffectFail(INTERNAL_EFFECT_KEY);
        return result(owner.getName() + " perd el control del conflicte intern i l'atac es desfà.");
    }

    /** Pot anul·lar el crític per falta d'estabilitat. */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!isAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        double severity = activeSeverity(owner);
        if (severity < MIN_ACTIVE_SEVERITY) {
            return EffectResult.none();
        }

        double chance = CRIT_BREAK_BASE + severity * CRIT_BREAK_SCALE;
        if (rng.nextDouble() >= chance) {
            return EffectResult.none();
        }

        ctx.forbidCritical();
        return result("El conflicte intern trenca la precisió crítica de " + owner.getName() + ".");
    }

    /** Redueix el dany quan les fonts de poder interfereixen. */
    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (!isAttacker(ctx, owner)) {
            return EffectResult.none();
        }

        double severity = activeSeverity(owner);
        if (severity < MIN_ACTIVE_SEVERITY) {
            return EffectResult.none();
        }

        double chance = DAMAGE_BREAK_BASE + severity * DAMAGE_BREAK_SCALE;
        if (rng.nextDouble() >= chance) {
            return EffectResult.none();
        }

        double multiplier = MAX_DAMAGE_MULTIPLIER - severity * 0.30;
        ctx.multiplyDamage(Math.clamp(multiplier, MIN_DAMAGE_MULTIPLIER, MAX_DAMAGE_MULTIPLIER));
        return result("Les fonts de poder xoquen i l'atac de " + owner.getName() + " perd força.");
    }

    @Override
    public int priority() {
        return 30;
    }

    private static boolean isAttacker(HitContext ctx, Character owner) {
        return ctx != null && owner != null && owner == ctx.attacker();
    }

    private static double activeSeverity(Character owner) {
        if (owner == null) {
            return 0.0;
        }
        return conflictSeverity(owner.getStatistics());
    }

    private static int rawConflictLoad(Statistics stats) {
        if (stats == null) {
            return 0;
        }
        return Math.min(stats.getStrength(), stats.getIntelligence()) - SAFE_LIMIT;
    }

    private static EffectResult result(String text) {
        return EffectResult.msg(CombatMessage.of(MessageSymbol.WARNING, MessageColor.RED, text));
    }
}