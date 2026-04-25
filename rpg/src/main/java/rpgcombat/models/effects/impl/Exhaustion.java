package rpgcombat.models.effects.impl;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Debuff curt que augmenta lleugerament el dany rebut i redueix l'esquiva.
 */
public class Exhaustion extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "EXHAUSTION";

    public static final double DODGE_MULTIPLIER = 0.90;

    private static final int TURNS = 2;
    private static final double INCOMING = 1.08;

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
    protected CombatMessage buildIncomingMessage(double multiplier, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.YELLOW,
                "El cansament fa que " + owner.getName() + " rebi una mica més de dany."
        );
    }
}