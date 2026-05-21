package rpgcombat.terrain.effects;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.types.EndRoundRecoveryEffect;
import rpgcombat.models.effects.types.RoundScopedEffect;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Adaptador que exposa un {@link Effect} com a delegat de terreny.
 */
final class EffectTerrainTriggerDelegate implements TerrainTriggerDelegate {
    private final Effect effect;

    /**
     * Crea l'adaptador del trigger personalitzat.
     *
     * @param effect efecte que executarà el terreny
     */
    EffectTerrainTriggerDelegate(Effect effect) {
        this.effect = effect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        return effect == null ? EffectResult.none() : effect.onPhase(ctx, phase, rng, owner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out) {
        if (effect instanceof RoundScopedEffect roundScoped) {
            roundScoped.onRoundStart(owner, roundNumber, rng, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRoundEnd(Character owner) {
        if (effect instanceof RoundScopedEffect roundScoped) {
            roundScoped.onRoundEnd(owner);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean suppressPassiveHealthRegen(Character owner) {
        return effect instanceof EndRoundRecoveryEffect recovery
                && recovery.suppressPassiveHealthRegen(owner);
    }
}
