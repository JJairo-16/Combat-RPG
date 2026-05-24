package rpgcombat.models.effects.types;

import rpgcombat.models.characters.Character;

/**
 * Efecte capaç d'exposar una pista curta abans de triar l'acció de combat.
 */
public interface ActionMenuHintEffect {
    /**
     * Retorna el text que el menú pot mostrar per a la ronda que començarà.
     *
     * @param owner combatent que conté l'efecte
     * @param nextRound número de ronda que es resoldrà després de triar accions
     * @return pista visible o text buit quan no cal mostrar-ne cap
     */
    String actionMenuHint(Character owner, int nextRound);
}
