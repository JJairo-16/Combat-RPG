package rpgcombat.combat.models;

import rpgcombat.combat.turnservice.TurnResult;

/**
 * Conté el resultat complet d'una ronda de combat.
 */
public record CombatRoundResult(
        TurnResult firstTurn,
        TurnResult secondTurn,
        double p1DamageTaken,
        double p2DamageTaken,
        RegenResult p1Regen,
        RegenResult p2Regen,
        Winner winner,
        CombatantStatus p1Initial,
        CombatantStatus p2Initial,
        CombatantStatus p1AfterDamage,
        CombatantStatus p2AfterDamage,
        CombatantStatus p1Final,
        CombatantStatus p2Final) {

    /**
     * Constructor compatible sense captures d'estat.
     */
    public CombatRoundResult(
            TurnResult firstTurn,
            TurnResult secondTurn,
            double p1DamageTaken,
            double p2DamageTaken,
            RegenResult p1Regen,
            RegenResult p2Regen,
            Winner winner) {
        this(firstTurn, secondTurn, p1DamageTaken, p2DamageTaken,
                p1Regen, p2Regen, winner, null, null, null, null, null, null);
    }
}
