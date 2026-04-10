package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Efecte que modifica el dany infligit, el rebut, o tots dos.
 *
 * Exemples:
 * - Frenesí: +25% dany infligit
 * - Resistència: -20% dany rebut
 * - Corromput: +15% dany infligit i +10% dany rebut
 */
public abstract class DamageModifierEffect extends TimedEffect {

    protected final double outgoingMultiplier;
    protected final double incomingMultiplier;

    /**
     * @param key clau única de l'efecte
     * @param turns duració en torns
     * @param outgoingMultiplier multiplicador del dany infligit pel portador
     *                           (1.0 = cap canvi, 1.25 = +25%, 0.8 = -20%)
     * @param incomingMultiplier multiplicador del dany rebut pel portador
     *                           (1.0 = cap canvi, 1.25 = +25%, 0.8 = -20%)
     */
    protected DamageModifierEffect(
            String key,
            int turns,
            double outgoingMultiplier,
            double incomingMultiplier
    ) {
        super(key, turns);

        if (outgoingMultiplier <= 0) {
            throw new IllegalArgumentException("El multiplicador de dany infligit ha de ser > 0");
        }
        if (incomingMultiplier <= 0) {
            throw new IllegalArgumentException("El multiplicador de dany rebut ha de ser > 0");
        }

        this.outgoingMultiplier = outgoingMultiplier;
        this.incomingMultiplier = incomingMultiplier;
    }

    /**
     * Hook opcional per restringir quan s'aplica l'efecte.
     * Per defecte, sempre s'aplica si el portador participa en el cop.
     */
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker() || owner == ctx.defender();
    }

   /** Hook opcional per ajustar dinàmicament el multiplicador del dany infligit. */
    protected double resolveOutgoingMultiplier(HitContext ctx, Random rng, Character owner) {
        return outgoingMultiplier;
    }

   /** Hook opcional per ajustar dinàmicament el multiplicador del dany rebut. */
    protected double resolveIncomingMultiplier(HitContext ctx, Random rng, Character owner) {
        return incomingMultiplier;
    }

   /** Missatge opcional quan modifica dany infligit. */
    protected String buildOutgoingMessage(double multiplier, Character owner) {
        return null;
    }

   /** Missatge opcional quan modifica dany rebut. */
    protected String buildIncomingMessage(double multiplier, Character owner) {
        return null;
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (isExpired() || !applies(ctx, owner)) {
            return EffectResult.none();
        }

        StringBuilder message = new StringBuilder();
        boolean changed = false;

        if (owner == ctx.attacker()) {
            double out = resolveOutgoingMultiplier(ctx, rng, owner);
            if (out > 0 && out != 1.0) {
                ctx.multiplyDamage(out);
                changed = true;

                String msg = buildOutgoingMessage(out, owner);
                if (msg != null && !msg.isBlank()) {
                    message.append(msg);
                }
            }
        }

        if (owner == ctx.defender()) {
            double in = resolveIncomingMultiplier(ctx, rng, owner);
            if (in > 0 && in != 1.0) {
                ctx.multiplyDamage(in);
                changed = true;

                String msg = buildIncomingMessage(in, owner);
                if (msg != null && !msg.isBlank()) {
                    if (!message.isEmpty()) {
                        message.append(" ");
                    }
                    message.append(msg);
                }
            }
        }

        if (!changed) {
            return EffectResult.none();
        }

        if (message.isEmpty()) {
            return EffectResult.changed(null);
        }

        return EffectResult.changed(message.toString());
    }

    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        if (isExpired()) {
            return EffectResult.none();
        }

        state.tickDuration();
        return EffectResult.none();
    }
}