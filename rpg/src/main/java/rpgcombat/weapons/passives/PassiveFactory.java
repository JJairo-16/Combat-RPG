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

        Map<String, Object> params = cfg.params();

        return switch (cfg.type()) {
            case "lifeSteal" -> Passives.lifeSteal(getDouble(params, "pct"));
            case "trueHarm" -> Passives.trueHarm(getDouble(params, "pct"));
            case "executor" -> Passives.executor(
                    getDouble(params, "thresholdLife"),
                    getDouble(params, "damageBonus"));
            case "blindOnHit" -> {
                double applyProb = getDouble(params, "applyProb");
                double missProb = getDouble(params, "missProb");
                int duration = getInteger(params, "duration");
                yield Passives.blindOnHit(applyProb, missProb, duration);
            }
            case "poisonChain" -> {
                double extraDamagePerStack = getDouble(params, "extraDamagePerStack");
                int softCapStart = getInteger(params, "softCapStart");
                double falloff = getDouble(params, "falloff");
                yield Passives.poisonChain(extraDamagePerStack, softCapStart, falloff);
            }
            case "elementalDuality" -> Passives.elementalDuality(
                    getDouble(params, "burnApplyProb"),
                    getInteger(params, "burnTurns"),
                    getDouble(params, "burnDamagePerTurn"),
                    getDouble(params, "frozenApplyProb"),
                    getInteger(params, "frozenTurns"),
                    getDouble(params, "frozenOutgoingMultiplier"),
                    getDouble(params, "frozenIncomingMultiplier"));
            case "firstOathClash" -> Passives.firstOathClash(getDouble(params, "damageBonus"));
            case "guardCounter" -> Passives.guardCounter(
                    getDouble(params, "damageBonus"),
                    getInteger(params, "minGuardStacks"));
            case "ancestralBell" -> Passives.ancestralBell(
                    getDouble(params, "damageBonus"),
                    getDouble(params, "healAmount"),
                    getInteger(params, "cooldownTurns"));
            case "tacticalMirror" -> Passives.tacticalMirror(getDouble(params, "damageBonus"));
            case "retaliationAgainstDefend" -> Passives.retaliationAgainstDefend(getDouble(params, "damageBonus"));
            case "badOmenCrit" -> Passives.badOmenCrit(
                    getDouble(params, "bonusPerStack"),
                    getInteger(params, "maxStacks"));
            case "chillOnHit" -> Passives.chillOnHit(
                    getDouble(params, "applyProb"),
                    getInteger(params, "turns"),
                    getDouble(params, "outgoingMultiplier"));
            default -> throw new IllegalArgumentException("Passiva desconeguda: " + cfg.type());
        };
    }

    private static int getInteger(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            throw new IllegalArgumentException("Falta el paràmetre: " + key);
        }

        Object value = params.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }

        throw new IllegalArgumentException("El paràmetre '" + key + "' no és numèric: " + value);
    }

    private static double getDouble(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            throw new IllegalArgumentException("Falta el paràmetre: " + key);
        }

        Object value = params.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }

        throw new IllegalArgumentException("El paràmetre '" + key + "' no és numèric: " + value);
    }
}