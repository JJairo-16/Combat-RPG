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
 * Encén una acció diferent cada ronda i recompensa qui la travessa.
 */
public final class DarkLanternsTerrainTrigger extends StoryTerrainTrigger
        implements RoundScopedEffect, ActionMenuHintEffect {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_DARK_LANTERNS";

    private static final int ACTION_SALT = 1;
    private Action litAction = roundAction(1, ACTION_SALT);
    private boolean litStrikeReady;

    /** Crea el trigger dels fanals apagats. */
    public DarkLanternsTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        litAction = roundAction(roundNumber, ACTION_SALT);
    }

    /**
     * Deixa el fanal preparat quan l'actor segueix l'acció il·luminada.
     */
    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        super.endTurn(ctx, rng, owner);
        if (!isActor(ctx, owner) || ctx.attackerAction() != litAction) {
            return EffectResult.none();
        }

        litStrikeReady = true;
        return terrainResult(MessageSymbol.POSITIVE,
                "Els Fanals Apagats il·luminen " + owner.getName()
                        + " i guarden llum per al seu proper atac.");
    }

    /**
     * Allibera la llum guardada sobre el proper atac del combatent.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!litStrikeReady || !isActor(ctx, owner)) {
            return EffectResult.none();
        }

        litStrikeReady = false;
        ctx.forceCritical();
        return terrainResult(MessageSymbol.INFO,
                "La llum dels Fanals Apagats troba el cop de " + owner.getName() + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionMenuHint(Character owner, int nextRound) {
        Action next = roundAction(nextRound, ACTION_SALT);
        return "Els Fanals Apagats\nL'acció il·luminada serà: " + next.label()
                + ". Seguir-la il·lumina el proper atac.";
    }
}
