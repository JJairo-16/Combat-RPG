package rpgcombat.models.effects.templates;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Efecte de tipus flag (booleà).
 *
 * Permet marcar un estat com:
 * - stun
 * - silenci
 * - invulnerable
 * - marcat
 *
 * No mostra missatge per defecte.
 */
public abstract class BooleanFlagEffect extends TimedEffect {

    private final String flagKey;
    private final boolean value;

    /**
     * @param effectKey clau de l'efecte
     * @param flagKey clau de la flag dins del context
     * @param turns duració
     * @param value valor de la flag (true/false)
     */
    protected BooleanFlagEffect(
            String effectKey,
            String flagKey,
            int turns,
            boolean value
    ) {
        super(effectKey, turns);

        if (flagKey == null || flagKey.isBlank()) {
            throw new IllegalArgumentException("La clau de la flag no pot ser nul·la");
        }

        this.flagKey = flagKey;
        this.value = value;
    }

    /**
     * Permet restringir quan s'aplica la flag.
     * Per defecte: sempre.
     */
    protected boolean applies(HitContext ctx, Character owner) {
        return true;
    }

    /**
     * Fase en què s'aplica la flag.
     * Per defecte: START_TURN (segur i previsible).
     */
    protected HitContext.Phase phase() {
        return HitContext.Phase.START_TURN;
    }

   /** Aplica la flag al context. */
    protected void applyFlag(HitContext ctx) {
        ctx.putMeta(flagKey, value);
    }

    @Override
    public EffectResult onPhase(HitContext ctx, HitContext.Phase phase, Random rng, Character owner) {

        if (isExpired()) {
            return EffectResult.none();
        }

        if (phase == phase() && applies(ctx, owner)) {
            applyFlag(ctx);
        }

        return EffectResult.none();
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