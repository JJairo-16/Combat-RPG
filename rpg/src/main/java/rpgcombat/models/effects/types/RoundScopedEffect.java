package rpgcombat.models.effects.types;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;

/** Efecte que pot preparar i netejar estat transitori al voltant d'una ronda completa. */
public interface RoundScopedEffect extends Effect {
    void onRoundStart(Character owner, int roundNumber, Random rng, CombatMessageBuffer out);

    default void onRoundEnd(Character owner) {
    }
}
