package rpgcombat.models.effects.impl;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.DamageModifierEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Congelació elemental: redueix el dany sortint i fa el portador lleugerament més fràgil.
 */
public final class FrozenEffect extends DamageModifierEffect {
    public static final String INTERNAL_EFFECT_KEY = "FROZEN";

    public FrozenEffect(int turns, double outgoingMultiplier, double incomingMultiplier) {
        super(INTERNAL_EFFECT_KEY, Math.max(1, turns), outgoingMultiplier, incomingMultiplier);
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        if (incoming instanceof FrozenEffect other) {
            state.refreshDuration(other.state().remainingTurns());
        }
    }

    @Override
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker() || owner == ctx.defender();
    }

    @Override
    protected CombatMessage buildOutgoingMessage(double multiplier, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.CYAN,
                "El gel entumeix l'atac de " + owner.getName() + "."
        );
    }

    @Override
    protected CombatMessage buildIncomingMessage(double multiplier, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.CYAN,
                owner.getName() + " està congelat i rep el cop amb el cos rígid."
        );
    }
}
