package rpgcombat.models.effects.impl;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;

/**
 * Efecte de verí acumulatiu amb soft cap suau.
 */
public class PoisonEffect implements Effect {
    public static final String INTERNAL_EFFECT_KEY = "POISON";

    private final EffectState state;
    private final double extraDamagePerStack;
    private final int softCapStart;
    private final double falloff;

    /** Crea un efecte de verí amb un nombre inicial de càrregues. */
    public PoisonEffect(double extraDamagePerStack, int softCapStart, double falloff, int initialStacks) {
        if (extraDamagePerStack < 0) {
            throw new IllegalArgumentException("El dany extra del verí no pot ser negatiu.");
        }
        if (softCapStart < 1) {
            throw new IllegalArgumentException("El soft cap ha de començar com a mínim a 1 stack.");
        }
        if (falloff <= 0) {
            throw new IllegalArgumentException("El factor de caiguda ha de ser major que 0.");
        }
        if (initialStacks < 1) {
            throw new IllegalArgumentException("L'efecte de verí ha de començar amb com a mínim 1 stack.");
        }

        this.state = EffectState.ofStacks(initialStacks);
        this.extraDamagePerStack = extraDamagePerStack;
        this.softCapStart = softCapStart;
        this.falloff = falloff;
    }

    @Override
    public String key() {
        return INTERNAL_EFFECT_KEY;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.STACK;
    }

    @Override
    public int maxStacks() {
        return Integer.MAX_VALUE;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public boolean isExpired() {
        return state.stacks() <= 0;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        if (!(incoming instanceof PoisonEffect other)) {
            return;
        }

        state.addStacks(other.state().stacks(), maxStacks());
    }

    /** Retorna els stacks actuals del verí. */
    public int stacks() {
        return state.stacks();
    }

    /** Elimina tot el verí acumulat. */
    public void clear() {
        state.setStacks(0);
    }

    /** Calcula el dany addicional segons els stacks actuals. */
    public double bonusDamage() {
        int stacks = state.stacks();
        if (stacks <= 0 || extraDamagePerStack <= 0) {
            return 0.0;
        }

        double effectiveStacks = effectiveStacksFor(stacks);
        return round2(effectiveStacks * extraDamagePerStack);
    }

    /**
     * Calcula els stacks efectius amb un creixement inicial controlat i un sostre
     * suau.
     *
     * <p>
     * Fins al llindar, el creixement és lineal. A partir d'allà, la corba
     * s'aplana progressivament fins a acostar-se a un màxim suau.
     * </p>
     */
    private double effectiveStacksFor(int stacks) {
        if (stacks <= softCapStart) {
            return stacks;
        }

        int overflow = stacks - softCapStart;

        double maxOverflowContribution = Math.max(1.0, softCapStart * 0.75);
        double overflowContribution = maxOverflowContribution * (1.0 - Math.exp(-falloff * overflow));

        return softCapStart + overflowContribution;
    }

    /** Cerca aquest efecte dins del personatge. */
    public static PoisonEffect from(Character character) {
        if (character == null) {
            return null;
        }

        Effect effect = character.getEffect(INTERNAL_EFFECT_KEY);
        return (effect instanceof PoisonEffect poison) ? poison : null;
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}