package rpgcombat.gamemode.chaos;

import java.util.Random;

import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.triggers.Chaos;

/** Aplica la política de Caos definida pel mode de joc, fora del motor cinematogràfic. */
public final class ChaosPolicy {
    private ChaosPolicy() {
    }

    public static boolean apply(GameModeDefinition mode, Character player1, Character player2, Random rng) {
        GameModeRules rules = mode == null ? GameModeRules.unrestricted() : mode.rules();
        if (!rules.chaos().shouldActivate(rng)) {
            return false;
        }

        if (player1 != null) {
            player1.addEffect(new Chaos());
        }
        if (player2 != null) {
            player2.addEffect(new Chaos());
        }
        return true;
    }
}
