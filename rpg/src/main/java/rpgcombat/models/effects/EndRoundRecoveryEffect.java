package rpgcombat.models.effects;

import rpgcombat.models.characters.Character;

/** Efecte capaç d'alterar la recuperació passiva de final de ronda. */
public interface EndRoundRecoveryEffect {
    default boolean suppressPassiveHealthRegen(Character owner) {
        return false;
    }
}
