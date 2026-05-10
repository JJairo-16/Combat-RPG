package rpgcombat.gamemode.effects;

import java.util.Map;

import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.triggers.BleedEmphasisTrigger;
import rpgcombat.models.effects.triggers.SelfDirectedAttackTrigger;

/** Crea efectos iniciales declarados por un modo de juego. */
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
            default -> throw new IllegalArgumentException("Efecto de modo desconocido: " + definition.id());
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
