package rpgcombat.balance.config;

/**
 * Configuració de l'anti-stall / sudden death.
 */
public record AntiStallConfig(
    int startTurn,
    int increaseEveryTurns,
    double initialDamage,
    double damageIncreasePerStep
) {}