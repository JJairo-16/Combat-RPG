package rpgcombat.models.effects.impl;

import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.models.characters.Character;

/**
 * Debuff curt que augmenta lleugerament el dany rebut i empitjora una mica l'esquiva.
 * Ha de castigar l'spam defensiu, pero sense inutilitzar-lo.
 */
public class Exhaustion extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "EXHAUSTION";

    // Abans: 0.82 (-18%)
    public static final double DODGE_MULTIPLIER = 0.90; // -10%

    private static final int TURNS = 2;

    // Abans: 1.12 (+12%)
    private static final double INCOMING = 1.08; // +8%

    public Exhaustion() {
        super(INTERNAL_EFFECT_KEY, TURNS, 1.0, INCOMING);
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.defender();
    }

    @Override
    protected String buildIncomingMessage(double multiplier, Character owner) {
        return "[YELLOW|-] El cansament fa que " + owner.getName() + " rebi una mica més de dany.";
    }
}