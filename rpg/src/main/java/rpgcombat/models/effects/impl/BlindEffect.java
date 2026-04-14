package rpgcombat.models.effects.impl;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.MissChanceEffect;
import rpgcombat.weapons.passives.HitContext;

/** Efecte de ceguesa temporal. */
public final class BlindEffect extends MissChanceEffect {

    private static final String KEY = "BLIND";
    private static final double DEFAULT_MISS_PROB = 0.20;

    private final double missProb;

    /** Crea una ceguesa amb probabilitat de fallada per defecte. */
    public BlindEffect(int turns) {
        this(turns, DEFAULT_MISS_PROB);
    }

    /** Crea una ceguesa amb durada i probabilitat de fallada configurables. */
    public BlindEffect(int turns, double missProb) {
        super(KEY, turns);

        if (missProb < 0.0 || missProb > 1.0) {
            throw new IllegalArgumentException("La probabilitat de fallada ha d'estar entre 0 i 1.");
        }

        this.missProb = missProb;
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        if (incoming instanceof BlindEffect other) {
            state.refreshDuration(other.state().remainingTurns());
        }
    }

    @Override
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker();
    }

    @Override
    protected double missChance(HitContext ctx, Random rng, Character owner) {
        return missProb;
    }

    @Override
    protected String buildMessage(Character owner) {
        return "[RED|-]" + owner.getName() + " està encegat i falla l'atac.";
    }

    @Override
    public int priority() {
        return 20;
    }
}