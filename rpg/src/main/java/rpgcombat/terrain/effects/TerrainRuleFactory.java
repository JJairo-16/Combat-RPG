package rpgcombat.terrain.effects;

import java.util.Map;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.terrain.model.TerrainRule;

/**
 * Converteix regles JSON de terreny en condicions i accions executables.
 */
final class TerrainRuleFactory {
    private TerrainRuleFactory() {
    }

    /**
     * Resol una condició del motor genèric.
     *
     * @param rule regla declarada al JSON
     * @return condició executable
     */
    static TerrainCondition condition(TerrainRule rule) {
        return switch (rule.type()) {
            case "CHANCE", "RANDOM_CHANCE" -> ctx -> safeRng(ctx).nextDouble() < chance(rule.params());
            case "OWNER_HEALTH_BELOW" -> ctx -> ctx.owner().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "TARGET_HEALTH_BELOW" -> ctx -> ctx.hit().defender().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "HAS_MOMENTUM" -> ctx -> ctx.owner().getMomentumStacks() >= (int) num(rule.params(), "min", 1);
            case "OWNER_ACTION_IS" -> ctx -> ownerAction(ctx) == action(rule.params(), "action");
            case "TARGET_ACTION_IS" -> ctx -> opponentAction(ctx) == action(rule.params(), "action");
            case "DAMAGE_AT_LEAST" -> ctx -> currentDamage(ctx) >= num(rule.params(), "amount", 0.0);
            case "OWNER_IS_ATTACKER" -> ctx -> ctx.owner() == ctx.hit().attacker();
            case "OWNER_IS_DEFENDER" -> ctx -> ctx.owner() == ctx.hit().defender();
            case "OWNER_ACTION_IN" -> ctx -> listContains(rule.params().get("actions"), ownerAction(ctx).name());
            case "OPPONENT_ACTION_IN" -> ctx -> listContains(rule.params().get("actions"), opponentAction(ctx).name());
            default -> ctx -> false;
        };
    }

    /**
     * Resol una acció del motor genèric.
     *
     * @param rule regla declarada al JSON
     * @return acció executable
     */
    static TerrainAction action(TerrainRule rule) {
        return switch (rule.type()) {
            case "MULTIPLY_DAMAGE" -> ctx -> {
                double multiplier = num(rule.params(), "multiplier", 1.0);
                ctx.hit().multiplyDamage(multiplier);
                if (multiplier == 1.0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.INFO, percentChangeText("modifica el dany", multiplier));
            };
            case "ADD_FLAT_DAMAGE" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0);
                ctx.hit().addFlatDamage(amount);
                if (amount == 0.0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.POSITIVE, (amount > 0 ? "+" : "") + round2(amount) + " de dany pla.");
            };
            case "ADD_CRIT_CHANCE" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0);
                ctx.hit().setCriticalChance(ctx.hit().criticalChance() + amount);
                if (amount == 0.0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.POSITIVE,
                        (amount > 0 ? "+" : "") + round2(amount * 100.0) + "% de probabilitat crítica.");
            };
            case "MULTIPLY_CRIT_DAMAGE" -> ctx -> {
                double multiplier = num(rule.params(), "multiplier", 1.0);
                ctx.hit().setCriticalMultiplier(ctx.hit().criticalMultiplier() * multiplier);
                if (multiplier == 1.0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.INFO, percentChangeText("modifica el dany crític", multiplier));
            };
            case "HEAL_OWNER" -> ctx -> {
                double amount = round2(num(rule.params(), "amount", 0.0)
                        + ctx.hit().damageDealt() * num(rule.params(), "ratioOfDamage", 0.0));
                if (amount <= 0) {
                    return EffectResult.none();
                }
                double healed = ctx.owner().getStatistics().heal(amount);
                if (healed <= 0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " recupera " + round2(healed) + " de vida.");
            };
            case "RESTORE_MANA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreMana(num(rule.params(), "amount", 0.0));
                if (restored <= 0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " recupera " + round2(restored) + " de mana.");
            };
            case "RESTORE_STAMINA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreStamina(num(rule.params(), "amount", 0.0));
                if (restored <= 0) {
                    return EffectResult.none();
                }
                return gamemode(MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " recupera " + round2(restored) + " d'estamina.");
            };
            case "FORCE_CRITICAL" -> ctx -> {
                ctx.hit().forceCritical();
                return gamemode(MessageSymbol.POSITIVE, str(rule.params(), "label", "El cop troba un angle crític."));
            };
            default -> ctx -> EffectResult.none();
        };
    }

    private static EffectResult gamemode(MessageSymbol symbol, String text) {
        return EffectResult.gamemode(MessageColor.CYAN, symbol, text);
    }

    private static Random safeRng(TerrainContext ctx) {
        return ctx.rng() == null ? new Random() : ctx.rng();
    }

    private static double chance(Map<String, Object> params) {
        if (params == null) {
            return 0.0;
        }
        if (params.containsKey("percent")) {
            return Math.clamp(num(params, "percent", 0.0) / 100.0, 0.0, 1.0);
        }
        if (params.containsKey("percentage")) {
            return Math.clamp(num(params, "percentage", 0.0) / 100.0, 0.0, 1.0);
        }
        if (params.containsKey("activationPercent")) {
            return Math.clamp(num(params, "activationPercent", 0.0) / 100.0, 0.0, 1.0);
        }
        return Math.clamp(num(params, "value", 0.0), 0.0, 1.0);
    }

    private static double currentDamage(TerrainContext ctx) {
        double dealt = ctx.hit().damageDealt();
        return dealt > 0 ? dealt : ctx.hit().damageToResolve();
    }

    private static Action ownerAction(TerrainContext ctx) {
        return ctx.owner() == ctx.hit().defender() ? ctx.hit().defenderAction() : ctx.hit().attackerAction();
    }

    private static Action opponentAction(TerrainContext ctx) {
        return ctx.owner() == ctx.hit().defender() ? ctx.hit().attackerAction() : ctx.hit().defenderAction();
    }

    private static String percentChangeText(String label, double multiplier) {
        double percent = round2((multiplier - 1.0) * 100.0);
        return label + " " + (percent > 0 ? "+" : "") + percent + "%.";
    }

    private static double num(Map<String, Object> params, String key, double def) {
        if (params == null) {
            return def;
        }
        Object value = params.get(key);
        return value instanceof Number n ? n.doubleValue() : def;
    }

    private static String str(Map<String, Object> params, String key, String def) {
        if (params == null) {
            return def;
        }
        Object value = params.get(key);
        return value == null ? def : value.toString();
    }

    private static boolean listContains(Object raw, String value) {
        if (raw instanceof Iterable<?> items) {
            for (Object item : items) {
                if (value.equals(String.valueOf(item))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Action action(Map<String, Object> params, String key) {
        try {
            return Action.valueOf(str(params, key, "ATTACK"));
        } catch (IllegalArgumentException ex) {
            return Action.ATTACK;
        }
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
