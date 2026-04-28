package rpgcombat.perks.effect;

import java.util.Map;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.Fatigue;
import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.weapons.passives.HitContext.Event;

/**
 * Fàbrica que converteix regles JSON en condicions i accions executables.
 */
final class PerkRuleFactory {
    private PerkRuleFactory() {
    }

    /**
     * Crea una condició a partir d'una regla.
     *
     * @param rule regla definida al JSON
     * @return condició executable
     */
    static PerkCondition condition(Rule rule) {
        return switch (rule.type()) {
            case "CHANCE" -> ctx -> ctx.rng().nextDouble() < num(rule.params(), "value", 0.0);
            case "OWNER_HEALTH_BELOW" -> ctx -> ctx.owner().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "TARGET_HEALTH_BELOW" -> ctx -> ctx.hit().defender().healthRatio() <= num(rule.params(), "ratio", 1.0);
            case "HAS_MOMENTUM" -> ctx -> ctx.owner().getMomentumStacks() >= (int) num(rule.params(), "min", 1);
            case "OWNER_ACTION_IS" -> ctx -> ownerAction(ctx) == action(rule.params(), "action");
            case "TARGET_ACTION_IS" -> ctx -> opponentAction(ctx) == action(rule.params(), "action");
            case "DAMAGE_AT_LEAST" -> ctx -> currentDamage(ctx) >= num(rule.params(), "amount", 0.0);
            case "EVENT" -> ctx -> ctx.hit().hasEvent(event(rule.params(), "event"));
            case "META_TRUE" -> ctx -> Boolean.TRUE.equals(ctx.hit().getMeta(str(rule.params(), "key", "")));
            case "OWNER_IS_ATTACKER" -> ctx -> ctx.owner() == ctx.hit().attacker();
            case "OWNER_IS_DEFENDER" -> ctx -> ctx.owner() == ctx.hit().defender();
            default -> ctx -> false;
        };
    }

    /**
     * Crea una acció a partir d'una regla.
     *
     * @param rule regla definida al JSON
     * @return acció executable
     */
    static PerkAction action(Rule rule) {
        return switch (rule.type()) {
            case "MULTIPLY_DAMAGE" -> ctx -> {
                double multiplier = num(rule.params(), "multiplier", 1.0);
                ctx.hit().multiplyDamage(multiplier);
                if (multiplier == 1.0) return EffectResult.none();
                return EffectResult.positive(percentChangeText("modifica el dany", multiplier));
            };
            case "ADD_FLAT_DAMAGE" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0);
                ctx.hit().addFlatDamage(amount);
                if (amount == 0.0) return EffectResult.none();
                return EffectResult.positive((amount > 0 ? "+" : "") + round2(amount) + " de dany pla.");
            };
            case "ADD_CRIT_CHANCE" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0);
                double current = ctx.hit().criticalChance();
                ctx.hit().setCriticalChance(current + amount);
                if (amount == 0.0) return EffectResult.none();
                return EffectResult.positive((amount > 0 ? "+" : "") + round2(amount * 100.0) + "% de probabilitat crítica.");
            };
            case "MULTIPLY_CRIT_DAMAGE" -> ctx -> {
                double multiplier = num(rule.params(), "multiplier", 1.0);
                double current = ctx.hit().criticalMultiplier();
                ctx.hit().setCriticalMultiplier(current * multiplier);
                if (multiplier == 1.0) return EffectResult.none();
                return EffectResult.positive(percentChangeText("modifica el dany crític", multiplier));
            };
            case "DEAL_EXTRA_DAMAGE" -> ctx -> {
                double extra = round2(ctx.hit().damageDealt() * num(rule.params(), "ratioOfLastDamage", 0.0));
                if (extra <= 0)
                    return EffectResult.none();
                ctx.hit().defender().getDamage(extra);
                return EffectResult.styled(MessageColor.YELLOW, MessageSymbol.POSITIVE,
                        str(rule.params(), "label", "un efecte") + ": +" + extra + " dany.");
            };
            case "HEAL_OWNER" -> ctx -> {
                double amount = num(rule.params(), "amount", 0.0)
                        + ctx.hit().damageDealt() * num(rule.params(), "ratioOfDamage", 0.0);
                amount = round2(amount);
                if (amount <= 0)
                    return EffectResult.none();
                double healed = ctx.owner().getStatistics().heal(amount);
                if (healed <= 0)
                    return EffectResult.positive(ctx.owner().getName() + " intenta robar vida, però ja està al màxim.");
                return EffectResult.positive(ctx.owner().getName() + " roba " + round2(healed) + " de vida.");
            };
            case "RESTORE_MANA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreMana(num(rule.params(), "amount", 0.0));
                if (restored <= 0)
                    return EffectResult.none();
                return EffectResult.positive(ctx.owner().getName() + " recupera " + round2(restored) + " de mana.");
            };
            case "RESTORE_STAMINA" -> ctx -> {
                double restored = ctx.owner().getStatistics().restoreStamina(num(rule.params(), "amount", 0.0));
                if (restored <= 0)
                    return EffectResult.none();
                return EffectResult.positive(ctx.owner().getName() + " recupera " + round2(restored) + " d'estamina.");
            };
            case "GAIN_MOMENTUM" -> ctx -> {
                int before = ctx.owner().getMomentumStacks();
                int amount = (int) num(rule.params(), "amount", 1);
                for (int i = 0; i < amount; i++)
                    ctx.owner().gainMomentum();
                if (ctx.owner().getMomentumStacks() <= before)
                    return EffectResult.none();
                return EffectResult.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                        ctx.owner().getName() + " guanya impuls.");
            };
            case "APPLY_STATUS" -> ctx -> {
                String status = str(rule.params(), "status", "");
                int turns = (int) num(rule.params(), "turns", 1);
                Character target = resolveTarget(ctx, str(rule.params(), "target", "TARGET"));
                switch (status) {
                    case "VULNERABLE" -> target.applyVulnerable(turns);
                    case "BLEED" -> target.applyBleed(turns);
                    case "STAGGER" -> target.applyStagger(turns);
                    case "BLIND" -> target.addEffect(new BlindEffect(turns));
                    case "FATIGUE" -> target.addEffect(new Fatigue(turns));
                    default -> {
                        return EffectResult.none();
                    }
                }
                return EffectResult.warning(target.getName() + " rep " + status.toLowerCase() + ".");
            };
            case "MULTIPLY_NEXT_INCOMING_DAMAGE" -> ctx -> {
                double multiplier = num(rule.params(), "multiplier", 1.0);
                ctx.owner().multiplyNextIncomingDamage(multiplier);
                if (multiplier == 1.0) return EffectResult.none();
                return EffectResult.warning(percentChangeText("modifica el proper dany rebut", multiplier));
            };
            case "SELF_DAMAGE" -> ctx -> {
                double maxRatio = num(rule.params(), "maxHealthRatio", 0.0);
                double amount = round2(
                        ctx.owner().getStatistics().getMaxHealth() * maxRatio + num(rule.params(), "amount", 0.0));
                if (amount <= 0)
                    return EffectResult.none();
                ctx.owner().getDamage(amount);
                return EffectResult.negative(ctx.owner().getName() + " paga " + amount + " de vida.");
            };
            default -> ctx -> EffectResult.none();
        };
    }

    /** Obté el dany ja resolt o, en fases prèvies, el dany actual que es resoldria. */
    private static double currentDamage(PerkContext ctx) {
        double dealt = ctx.hit().damageDealt();
        if (dealt > 0) return dealt;
        return ctx.hit().damageToResolve();
    }

    /** Obté l'acció triada pel propietari real de la perk. */
    private static Action ownerAction(PerkContext ctx) {
        return ctx.owner() == ctx.hit().defender()
                ? ctx.hit().defenderAction()
                : ctx.hit().attackerAction();
    }

    /** Obté l'acció triada per l'oponent del propietari de la perk. */
    private static Action opponentAction(PerkContext ctx) {
        return ctx.owner() == ctx.hit().defender()
                ? ctx.hit().attackerAction()
                : ctx.hit().defenderAction();
    }

    /** Resol el destinatari d'una acció de perk. */
    private static Character resolveTarget(PerkContext ctx, String target) {
        return switch (target) {
            case "OWNER" -> ctx.owner();
            case "OPPONENT" -> ctx.owner() == ctx.hit().attacker() ? ctx.hit().defender() : ctx.hit().attacker();
            case "ATTACKER" -> ctx.hit().attacker();
            case "DEFENDER", "TARGET" -> ctx.hit().defender();
            default -> ctx.hit().defender();
        };
    }

    /** Text curt per a multiplicadors percentuals. */
    private static String percentChangeText(String label, double multiplier) {
        double percent = round2((multiplier - 1.0) * 100.0);
        return label + " " + (percent > 0 ? "+" : "") + percent + "%.";
    }

    /** Obté un valor numèric dels paràmetres. */
    private static double num(Map<String, Object> params, String key, double def) {
        if (params == null) return def;
        Object value = params.get(key);
        return value instanceof Number n ? n.doubleValue() : def;
    }

    /** Obté un text dels paràmetres. */
    private static String str(Map<String, Object> params, String key, String def) {
        if (params == null) return def;
        Object value = params.get(key);
        return value == null ? def : value.toString();
    }

    /** Converteix un paràmetre a acció. */
    private static Action action(Map<String, Object> params, String key) {
        try {
            return Action.valueOf(str(params, key, "ATTACK"));
        } catch (IllegalArgumentException ex) {
            return Action.ATTACK;
        }
    }

    /** Converteix un paràmetre a esdeveniment. */
    private static Event event(Map<String, Object> params, String key) {
        try {
            return Event.valueOf(str(params, key, "ON_HIT"));
        } catch (IllegalArgumentException ex) {
            return Event.ON_HIT;
        }
    }

    /** Arrodoneix a 2 decimals. */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
