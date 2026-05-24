package rpgcombat.terrain.effects.triggers;

import java.util.Random;

import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.types.ActionMenuHintEffect;
import rpgcombat.weapons.passives.HitContext;

/**
 * Tensa les cadenes quan les accions divergeixen i arrossega el següent cop.
 */
public final class ThresholdChainsTerrainTrigger extends StoryTerrainTrigger implements ActionMenuHintEffect {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_THRESHOLD_CHAINS";

    private static final int PULL_THRESHOLD = 2;

    private int tension;
    private boolean pullReady;

    /** Crea el trigger de les cadenes del llindar. */
    public ThresholdChainsTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * Acumula tensió quan la ronda separa les decisions dels combatents.
     */
    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        super.endTurn(ctx, rng, owner);
        if (!isActor(ctx, owner)) {
            return EffectResult.none();
        }
        if (!actionsDiffer(ctx)) {
            tension = 0;
            return EffectResult.none();
        }

        tension++;
        if (tension < PULL_THRESHOLD) {
            return EffectResult.none();
        }

        tension = 0;
        pullReady = true;
        return terrainResult(MessageSymbol.WARNING,
                "Les Cadenes del Llindar arrosseguen " + owner.getName()
                        + " cap al seu proper atac.");
    }

    /**
     * Allibera l'estrebada sobre el següent atac del combatent tensat.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!pullReady || !isActor(ctx, owner)) {
            return EffectResult.none();
        }

        pullReady = false;
        ctx.forceCritical();
        return terrainResult(MessageSymbol.POSITIVE,
                "L'estrebada de les cadenes duu el cop de " + owner.getName() + " fins al llindar.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionMenuHint(Character owner, int nextRound) {
        if (pullReady) {
            return "Les Cadenes del Llindar\nL'estrebada ja espera el proper atac.";
        }
        if (tension <= 0) {
            return "";
        }
        return "Les Cadenes del Llindar\nTensió: " + tension + "/" + PULL_THRESHOLD
                + ". Triar diferent un cop més prepara l'estrebada.";
    }
}
