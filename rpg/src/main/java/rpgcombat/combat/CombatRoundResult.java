package rpgcombat.combat;

import rpgcombat.combat.turnservice.TurnResult;

/**
 * Conté el resultat complet d’una ronda de combat.
 *
 * @param firstTurn resultat del primer torn
 * @param secondTurn resultat del segon torn
 * @param p1DamageTaken dany rebut pel jugador 1
 * @param p2DamageTaken dany rebut pel jugador 2
 * @param p1Regen regeneració del jugador 1
 * @param p2Regen regeneració del jugador 2
 * @param winner guanyador de la ronda o del combat
 */
public record CombatRoundResult(
        TurnResult firstTurn,
        TurnResult secondTurn,
        double p1DamageTaken,
        double p2DamageTaken,
        RegenResult p1Regen,
        RegenResult p2Regen,
        Winner winner) {
}