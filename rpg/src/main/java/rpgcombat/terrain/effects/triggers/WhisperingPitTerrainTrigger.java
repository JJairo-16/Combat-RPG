package rpgcombat.terrain.effects.triggers;

import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.types.ActionMenuHintEffect;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Murmura una acció compartida i obre un eco si tots dos l'obeeixen.
 */
public final class WhisperingPitTerrainTrigger extends StoryTerrainTrigger
        implements RoundScopedEffect, ActionMenuHintEffect {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_WHISPERING_PIT";

    private static final int ACTION_SALT = 3;
    private Action whisperedAction = roundAction(1, ACTION_SALT);
    private boolean chorusReady;

    /** Crea el trigger del fossar de les veus. */
    public WhisperingPitTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        whisperedAction = roundAction(roundNumber, ACTION_SALT);
    }

    /**
     * Fa que el cor del fossar premiï l'obediència conjunta.
     */
    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        super.endTurn(ctx, rng, owner);
        if (!isActor(ctx, owner) || !bothChose(ctx, whisperedAction)) {
            return EffectResult.none();
        }

        chorusReady = true;
        return terrainResult(MessageSymbol.INFO,
                "El Fossar de les Veus reconeix el cor de " + owner.getName()
                        + " i guarda un eco per al seu proper atac.");
    }

    /**
     * Obre el següent atac quan el combatent conserva un eco del fossar.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!chorusReady || !isActor(ctx, owner)) {
            return EffectResult.none();
        }

        chorusReady = false;
        ctx.forceCritical();
        return terrainResult(MessageSymbol.POSITIVE,
                "L'eco del Fossar de les Veus guia el cop de " + owner.getName() + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionMenuHint(Character owner, int nextRound) {
        Action next = roundAction(nextRound, ACTION_SALT);
        return "El Fossar de les Veus\nMurmura: " + next.label() + ".";
    }
}
