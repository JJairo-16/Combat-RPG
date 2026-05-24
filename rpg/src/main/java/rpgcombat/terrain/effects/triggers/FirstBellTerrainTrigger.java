package rpgcombat.terrain.effects.triggers;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Fa que el primer impacte net de cada combatent guanyi impuls.
 */
public final class FirstBellTerrainTrigger extends StoryTerrainTrigger implements RoundScopedEffect {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_FIRST_BELL";

    private boolean firstHitPending = true;

    /** Crea el trigger de la campana del primer cop. */
    public FirstBellTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        firstHitPending = true;
    }

    /**
     * Dona impuls quan la campana respon al primer impacte.
     */
    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        if (!firstHitPending || !isActor(ctx, owner)) {
            return EffectResult.none();
        }

        firstHitPending = false;
        owner.gainMomentum();
        return terrainResult(MessageSymbol.POSITIVE,
                "La Campana del Primer Cop sona per " + owner.getName()
                        + " i li deixa impuls.");
    }
}
