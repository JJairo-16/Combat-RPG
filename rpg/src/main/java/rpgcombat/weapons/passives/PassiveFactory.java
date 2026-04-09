package rpgcombat.weapons.passives;

import java.util.Map;

import rpgcombat.weapons.config.PassiveConfig;

/**
 * Construeix passives reals a partir de la configuració JSON.
 */
public final class PassiveFactory {
    private PassiveFactory() {
    }

    public static WeaponPassive create(PassiveConfig cfg) {
        if (cfg == null || cfg.type() == null || cfg.type().isBlank()) {
            throw new IllegalArgumentException("Passiva invàlida o sense tipus: " + cfg);
        }

        return switch (cfg.type()) {
            case "lifeSteal" -> Passives.lifeSteal(getDouble(cfg.params(), "pct"));
            case "trueHarm" -> Passives.trueHarm(getDouble(cfg.params(), "pct"));
            case "executor" -> Passives.executor(
                    getDouble(cfg.params(), "thresholdLife"),
                    getDouble(cfg.params(), "damageBonus"));
            default -> throw new IllegalArgumentException("Passiva desconeguda: " + cfg.type());
        };
    }

    private static double getDouble(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            throw new IllegalArgumentException("Falta el paràmetre: " + key);
        }

        Object value = params.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }

        throw new IllegalArgumentException("El paràmetre '" + key + "' no es numèric: " + value);
    }
}
