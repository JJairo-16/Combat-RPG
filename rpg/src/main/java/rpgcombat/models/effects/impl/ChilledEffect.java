package rpgcombat.models.effects.impl;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;

/** Fred menor: redueix lleugerament el dany sortint del portador. */
public final class ChilledEffect extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "CHILLED";

    public ChilledEffect(int turns, double outgoingMultiplier) {
        super(INTERNAL_EFFECT_KEY, Math.max(1, turns), outgoingMultiplier, 1.0);
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        if (incoming instanceof ChilledEffect other) {
            state.refreshDuration(other.state().remainingTurns());
        }
    }

    @Override
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker();
    }

    @Override
    protected CombatMessage buildOutgoingMessage(double multiplier, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.CYAN,
                "El fred menor fa més pesat l'atac de " + owner.getName() + ".");
    }
}
