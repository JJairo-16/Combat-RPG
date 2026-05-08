package rpgcombat.models.effects.impl.elemental;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.models.effects.templates.ConstantDamageEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Cremada elemental: causa dany constant al final del torn del portador.
 */
public final class BurnEffect extends ConstantDamageEffect {
    public static final String INTERNAL_EFFECT_KEY = "BURN";

    public BurnEffect(int turns, double damagePerTurn) {
        super(INTERNAL_EFFECT_KEY, Math.max(1, turns), damagePerTurn);
    }

    @Override
    public StackingRule stackingRule() {
        return StackingRule.REFRESH;
    }

    @Override
    public void mergeFrom(Effect incoming) {
        if (incoming instanceof BurnEffect other) {
            state.refreshDuration(other.state().remainingTurns());
        }
    }

    @Override
    protected double resolveTickDamage(HitContext ctx, Random rng, Character owner) {
        return round2(damagePerTurn);
    }

    @Override
    protected CombatMessage buildMessage(double appliedDamage, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.RED,
                owner.getName() + " pateix " + appliedDamage + " de dany per cremada."
        );
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}
