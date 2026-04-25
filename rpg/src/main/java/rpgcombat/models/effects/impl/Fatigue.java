package rpgcombat.models.effects.impl;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Debuff curt que redueix lleugerament el dany infligit.
 */
public class Fatigue extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "FATIGUE";

    private static final int DEFAULT_TURNS = 2;
    private static final double OUTGOING = 0.92;

    public Fatigue() {
        this(DEFAULT_TURNS);
    }

    public Fatigue(int turns) {
        super(INTERNAL_EFFECT_KEY, Math.max(0, turns), OUTGOING, 1.0);
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
    protected CombatMessage buildOutgoingMessage(double multiplier, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.YELLOW,
                "La fatiga rebaixa una mica el dany de " + owner.getName() + "."
        );
    }
}