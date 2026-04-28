package rpgcombat.models.effects.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Efecte que modifica el dany infligit, el rebut, o tots dos.
 */
public abstract class DamageModifierEffect extends TimedEffect {

    protected final double outgoingMultiplier;
    protected final double incomingMultiplier;

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
     */
    protected boolean applies(HitContext ctx, Character owner) {
        return owner == ctx.attacker() || owner == ctx.defender();
    }

    /** Hook opcional per ajustar el multiplicador del dany infligit. */
    protected double resolveOutgoingMultiplier(HitContext ctx, Random rng, Character owner) {
        return outgoingMultiplier;
    }

    /** Hook opcional per ajustar el multiplicador del dany rebut. */
    protected double resolveIncomingMultiplier(HitContext ctx, Random rng, Character owner) {
        return incomingMultiplier;
    }

    /** Missatge opcional quan modifica dany infligit. */
    protected CombatMessage buildOutgoingMessage(double multiplier, Character owner) {
        return null;
    }

    /** Missatge opcional quan modifica dany rebut. */
    protected CombatMessage buildIncomingMessage(double multiplier, Character owner) {
        return null;
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (isExpired() || !applies(ctx, owner)) {
            return EffectResult.none();
        }

        List<CombatMessage> messages = new ArrayList<>();
        boolean changed = false;

        if (owner == ctx.attacker()) {
            double out = resolveOutgoingMultiplier(ctx, rng, owner);
            if (out > 0 && out != 1.0) {
                ctx.multiplyDamage(out);
                changed = true;

                CombatMessage msg = buildOutgoingMessage(out, owner);
                if (msg != null && !msg.text().isBlank()) {
                    messages.add(msg);
                }
            }
        }

        if (owner == ctx.defender()) {
            double in = resolveIncomingMultiplier(ctx, rng, owner);
            if (in > 0 && in != 1.0) {
                ctx.multiplyDamage(in);
                changed = true;

                CombatMessage msg = buildIncomingMessage(in, owner);
                if (msg != null && !msg.text().isBlank()) {
                    messages.add(msg);
                }
            }
        }

        if (!changed) {
            return EffectResult.none();
        }

        if (messages.isEmpty()) {
            return EffectResult.changed(null);
        }

        if (messages.size() == 1) {
            return EffectResult.changed(messages.get(0));
        }

        return EffectResult.changed(mergeMessages(messages));
    }

    private CombatMessage mergeMessages(List<CombatMessage> messages) {
        CombatMessage first = messages.get(0);

        String text = messages.stream()
                .map(CombatMessage::text)
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        return CombatMessage.of(
                first.symbol(),
                first.color(),
                text
        );
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