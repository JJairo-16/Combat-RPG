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
 * Alterna rondes d'inhalació i exhalació per donar ritme a la porta.
 */
public final class BreathingGateTerrainTrigger extends StoryTerrainTrigger
        implements RoundScopedEffect, ActionMenuHintEffect {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_BREATHING_GATE";

    private boolean inhaling = true;
    private boolean breathHeld;

    /** Crea el trigger de la porta que respira. */
    public BreathingGateTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        inhaling = isInhaling(roundNumber);
    }

    /**
     * Guarda alè en inhalació o el deixa perdre si l'exhalació passa sense atac.
     */
    @Override
    public EffectResult endTurn(HitContext ctx, Random rng, Character owner) {
        super.endTurn(ctx, rng, owner);
        if (!isActor(ctx, owner)) {
            return EffectResult.none();
        }

        if (inhaling && ctx.attackerAction() != Action.ATTACK) {
            breathHeld = true;
            return terrainResult(MessageSymbol.INFO,
                    "La Porta que Respira guarda l'alè de " + owner.getName() + ".");
        }

        if (!inhaling && ctx.attackerAction() != Action.ATTACK && breathHeld) {
            breathHeld = false;
            return terrainResult(MessageSymbol.EQUAL,
                    "La Porta que Respira deixa escapar l'alè que " + owner.getName() + " no ha alliberat.");
        }

        return EffectResult.none();
    }

    /**
     * Allibera l'alè guardat sobre un atac de ronda d'exhalació.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (inhaling || !breathHeld || !isActor(ctx, owner)) {
            return EffectResult.none();
        }

        breathHeld = false;
        ctx.forceCritical();
        return terrainResult(MessageSymbol.POSITIVE,
                "La Porta que Respira exhala dins el cop de " + owner.getName() + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String actionMenuHint(Character owner, int nextRound) {
        if (isInhaling(nextRound)) {
            return "La Porta que Respira\nInhala: defensar, esquivar o carregar pot guardar alè.";
        }
        return "La Porta que Respira\nExhala: un atac pot alliberar l'alè guardat.";
    }

    /**
     * Resol l'estat de respiració d'una ronda.
     *
     * @param roundNumber ronda consultada
     * @return {@code true} quan la porta inhala
     */
    private boolean isInhaling(int roundNumber) {
        return Math.max(1, roundNumber) % 2 == 1;
    }
}
