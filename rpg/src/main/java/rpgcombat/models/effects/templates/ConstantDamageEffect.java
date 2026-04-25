package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Base per a efectes de dany constant per torn.
 *
 * Exemples:
 * - verí
 * - cremada
 * - sagnat
 *
 * Aplica dany al portador al final del torn.
 */
public abstract class ConstantDamageEffect extends TimedEffect {

    protected final double damagePerTurn;

    protected ConstantDamageEffect(String key, int turns, double damagePerTurn) {
        super(key, turns);

        if (damagePerTurn < 0) {
            throw new IllegalArgumentException("El dany per torn no pot ser negatiu");
        }

        this.damagePerTurn = damagePerTurn;
    }

    /** Permet modificar el dany del tick (per stats, RNG, etc.). */
    protected double resolveTickDamage(HitContext ctx, Random rng, Character owner) {
        return damagePerTurn;
    }

    /** Missatge per defecte. */
    protected CombatMessage buildMessage(double appliedDamage, Character owner) {
        return CombatMessage.of(
                MessageSymbol.NEGATIVE,
                MessageColor.RED,
                owner.getName() + " rep " + appliedDamage + " de dany per efecte.");
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        if (isExpired()) {
            return EffectResult.none();
        }

        double damage = Math.max(0.0, resolveTickDamage(ctx, rng, owner));

        if (damage > 0) {
            owner.getStatistics().damage(damage);
        }

        state.tickDuration();

        if (damage > 0) {
            return EffectResult.changed(buildMessage(damage, owner));
        }

        return EffectResult.changed(null);
    }
}