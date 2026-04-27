package rpgcombat.perks.effect;

import java.util.Map;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.Fatigue;
import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.weapons.passives.HitContext.Event;

/** Crea condicions i accions genèriques a partir de JSON. */
final class PerkRuleFactory {
    private PerkRuleFactory() {}

    static PerkCondition condition(Rule rule) {
        return switch (rule.type()) {
            case "CHANCE" -> ctx -> ctx.rng().nextDouble() < num(rule.params(), "value", 0.0);
            case "OWNER_HEALTH_BELOW" -> ctx -> ctx.owner().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "TARGET_HEALTH_BELOW" -> ctx -> ctx.hit().defender().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "HAS_MOMENTUM" -> ctx -> ctx.owner().getMomentumStacks() >= (int) num(rule.params(), "min", 1);
            case "OWNER_ACTION_IS" -> ctx -> ctx.hit().attackerAction() == action(rule.params(), "action");
            case "TARGET_ACTION_IS" -> ctx -> ctx.hit().defenderAction() == action(rule.params(), "action");
            case "DAMAGE_AT_LEAST" -> ctx -> ctx.hit().damageDealt() >= num(rule.params(), "amount", 0.0);
            case "EVENT" -> ctx -> ctx.hit().hasEvent(event(rule.params(), "event"));
            case "META_TRUE" -> ctx -> Boolean.TRUE.equals(ctx.hit().getMeta(str(rule.params(), "key", "")));
            case "OWNER_IS_ATTACKER" -> ctx -> ctx.owner() == ctx.hit().attacker();
            case "OWNER_IS_DEFENDER" -> ctx -> ctx.owner() == ctx.hit().defender();
            default -> ctx -> false;
        };
    }

    static PerkAction action(Rule rule) {
        return switch (rule.type()) {
            case "MULTIPLY_DAMAGE" -> ctx -> {
                ctx.hit().multiplyDamage(num(rule.params(), "multiplier", 1.0));
                return EffectResult.none();
            };
            case "ADD_FLAT_DAMAGE" -> ctx -> {
                ctx.hit().addFlatDamage(num(rule.params(), "amount", 0.0));
                return EffectResult.none();
            };
            case "ADD_CRIT_CHANCE" -> ctx -> {
                double current = ctx.hit().criticalChance();
                ctx.hit().setCriticalChance(current + num(rule.params(), "amount", 0.0));
                return EffectResult.none();
            };
            case "MULTIPLY_CRIT_DAMAGE" -> ctx -> {
                double current = ctx.hit().criticalMultiplier();
                ctx.hit().setCriticalMultiplier(current * num(rule.params(), "multiplier", 1.0));
                return EffectResult.none();
            };
            case "DEAL_EXTRA_DAMAGE" -> ctx -> {
                double extra = round2(ctx.hit().damageDealt() * num(rule.params(), "ratioOfLastDamage", 0.0));
                if (extra <= 0) return EffectResult.none();
                ctx.hit().defender().getDamage(extra);
                return EffectResult.styled(MessageColor.YELLOW, MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " activa " + str(rule.params(), "label", "un efecte") + ": +" + extra + " dany.");
            };
            case "HEAL_OWNER" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0)
                        + ctx.hit().damageDealt() * num(rule.params(), "ratioOfDamage", 0.0);
                double healed = ctx.owner().getStatistics().heal(round2(amount));
                if (healed <= 0) return EffectResult.none();
                return EffectResult.positive(ctx.owner().getName() + " recupera " + round2(healed) + " de vida.");
            };
            case "RESTORE_MANA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreMana(num(rule.params(), "amount", 0.0));
                if (restored <= 0) return EffectResult.none();
                return EffectResult.positive(ctx.owner().getName() + " recupera " + round2(restored) + " de mana.");
            };
            case "RESTORE_STAMINA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreStamina(num(rule.params(), "amount", 0.0));
                if (restored <= 0) return EffectResult.none();
                return EffectResult.positive(ctx.owner().getName() + " recupera " + round2(restored) + " d'estamina.");
            };
            case "GAIN_MOMENTUM" -> ctx -> {
                int before = ctx.owner().getMomentumStacks();
                int amount = (int) num(rule.params(), "amount", 1);
                for (int i = 0; i < amount; i++) ctx.owner().gainMomentum();
                if (ctx.owner().getMomentumStacks() <= before) return EffectResult.none();
                return EffectResult.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " guanya impuls.");
            };
            case "APPLY_STATUS" -> ctx -> {
                String status = str(rule.params(), "status", "");
                int turns = (int) num(rule.params(), "turns", 1);
                var target = "OWNER".equals(str(rule.params(), "target", "TARGET")) ? ctx.owner() : ctx.hit().defender();
                switch (status) {
                    case "VULNERABLE" -> target.applyVulnerable(turns);
                    case "BLEED" -> target.applyBleed(turns);
                    case "STAGGER" -> target.applyStagger(turns);
                    case "BLIND" -> target.addEffect(new BlindEffect(turns));
                    case "FATIGUE" -> target.addEffect(new Fatigue(turns));
                    default -> { return EffectResult.none(); }
                }
                return EffectResult.warning(target.getName() + " rep " + status.toLowerCase() + ".");
            };
            case "MULTIPLY_NEXT_INCOMING_DAMAGE" -> ctx -> {
                ctx.owner().multiplyNextIncomingDamage(num(rule.params(), "multiplier", 1.0));
                return EffectResult.none();
            };
            case "SELF_DAMAGE" -> ctx -> {
                double maxRatio = num(rule.params(), "maxHealthRatio", 0.0);
                double amount = round2(ctx.owner().getStatistics().getMaxHealth() * maxRatio + num(rule.params(), "amount", 0.0));
                if (amount <= 0) return EffectResult.none();
                ctx.owner().getDamage(amount);
                return EffectResult.negative(ctx.owner().getName() + " paga " + amount + " de vida.");
            };
            default -> ctx -> EffectResult.none();
        };
    }

    private static double num(Map<String, Object> params, String key, double def) {
        Object value = params.get(key);
        return value instanceof Number n ? n.doubleValue() : def;
    }

    private static String str(Map<String, Object> params, String key, String def) {
        Object value = params.get(key);
        return value == null ? def : value.toString();
    }

    private static Action action(Map<String, Object> params, String key) {
        return Action.valueOf(str(params, key, "ATTACK"));
    }

    private static Event event(Map<String, Object> params, String key) {
        return Event.valueOf(str(params, key, "ON_HIT"));
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
