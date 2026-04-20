package rpgcombat.models.effects.impl;

import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.models.characters.Character;

/**
 * Debuff curt que redueix lleugerament el dany infligit.
 * Ha de castigar l'spam ofensiu, pero sense enfonsar el personatge.
 */
public class Fatigue extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "FATIGUE";

    private static final int TURNS = 2;

    // Abans: 0.88 (-12%)
    private static final double OUTGOING = 0.92; // -8%

    public Fatigue() {
        super(INTERNAL_EFFECT_KEY, TURNS, OUTGOING, 1.0);
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker();
    }

    @Override
    protected String buildOutgoingMessage(double multiplier, Character owner) {
        return "[YELLOW|-] La fatiga rebaixa una mica el dany de " + owner.getName() + ".";
    }
}