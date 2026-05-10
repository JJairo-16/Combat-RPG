package rpgcombat.gamemode.effects;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.gamemode.model.GameModeRules;

/** Aplica a los jugadores los efectos iniciales definidos por un modo. */
public final class ModeEffectApplier {
    private ModeEffectApplier() {
    }

    public static void apply(GameModeRules rules, Character player1, Character player2) {
        GameModeRules effectiveRules = rules == null ? GameModeRules.unrestricted() : rules;
        for (ModeEffectDefinition definition : effectiveRules.modeEffects()) {
            apply(definition, player1, player2);
        }
    }

    private static void apply(ModeEffectDefinition definition, Character player1, Character player2) {
        if (definition == null) {
            return;
        }
        if (appliesToPlayer1(definition.target()) && player1 != null) {
            addFreshEffect(player1, definition);
        }
        if (appliesToPlayer2(definition.target()) && player2 != null) {
            addFreshEffect(player2, definition);
        }
    }

    private static boolean appliesToPlayer1(ModeEffectTarget target) {
        return target == ModeEffectTarget.BOTH || target == ModeEffectTarget.PLAYER1;
    }

    private static boolean appliesToPlayer2(ModeEffectTarget target) {
        return target == ModeEffectTarget.BOTH || target == ModeEffectTarget.PLAYER2;
    }

    private static void addFreshEffect(Character player, ModeEffectDefinition definition) {
        Effect effect = ModeEffectFactory.create(definition);
        if (effect != null) {
            player.addEffect(effect);
        }
    }
}
