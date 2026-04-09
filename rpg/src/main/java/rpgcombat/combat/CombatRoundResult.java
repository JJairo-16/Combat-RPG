package rpgcombat.combat;

import rpgcombat.combat.turnservice.TurnResult;

public record CombatRoundResult(
        TurnResult firstTurn,
        TurnResult secondTurn,
        double p1DamageTaken,
        double p2DamageTaken,
        RegenResult p1Regen,
        RegenResult p2Regen,
        Winner winner) {
}