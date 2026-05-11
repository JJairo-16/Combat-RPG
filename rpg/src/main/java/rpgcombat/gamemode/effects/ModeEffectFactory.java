package rpgcombat.gamemode.effects;

import java.util.Map;

import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.triggers.BleedEmphasisTrigger;
import rpgcombat.models.effects.triggers.SelfDirectedAttackTrigger;
import rpgcombat.models.effects.triggers.UniversalLifeStealTrigger;

/** Crea els efectes inicials declarats per un mode de joc. */
public final class ModeEffectFactory {
    private ModeEffectFactory() {
    }

    public static Effect create(ModeEffectDefinition definition) {
        if (definition == null) {
            return null;
        }

        Map<String, Double> params = definition.parameters();
        return switch (definition.id()) {
            case BleedEmphasisTrigger.INTERNAL_EFFECT_KEY -> new BleedEmphasisTrigger(
                    number(params, "damageMultiplier", BleedEmphasisTrigger.DEFAULT_DAMAGE_MULTIPLIER),
                    integer(params, "criticalBleedTurns", BleedEmphasisTrigger.DEFAULT_CRITICAL_BLEED_TURNS),
                    integer(params, "deepCutBleedTurns", BleedEmphasisTrigger.DEFAULT_DEEP_CUT_BLEED_TURNS));
            case SelfDirectedAttackTrigger.INTERNAL_EFFECT_KEY -> new SelfDirectedAttackTrigger(
                    number(params, "damageMultiplier", SelfDirectedAttackTrigger.DEFAULT_DAMAGE_MULTIPLIER),
                    bool(params, "canKill", SelfDirectedAttackTrigger.DEFAULT_CAN_KILL));
            case UniversalLifeStealTrigger.INTERNAL_EFFECT_KEY -> new UniversalLifeStealTrigger(
                    number(params, "lifeStealPct", UniversalLifeStealTrigger.DEFAULT_LIFE_STEAL_PCT),
                    number(params, "maxHealPct", UniversalLifeStealTrigger.DEFAULT_MAX_HEAL_PCT),
                    bool(params, "suppressPassiveHealthRegen",
                            UniversalLifeStealTrigger.DEFAULT_SUPPRESS_PASSIVE_HEALTH_REGEN));
            default -> throw new IllegalArgumentException("Efecte de mode desconegut: " + definition.id());
        };
    }

    private static double number(Map<String, Double> params, String key, double fallback) {
        if (params == null || key == null) {
            return fallback;
        }
        return params.getOrDefault(key, fallback);
    }

    private static int integer(Map<String, Double> params, String key, int fallback) {
        return (int) Math.round(number(params, key, fallback));
    }

    private static boolean bool(Map<String, Double> params, String key, boolean fallback) {
        if (params == null || key == null || !params.containsKey(key)) {
            return fallback;
        }
        return params.get(key) > 0.0;
    }
}
