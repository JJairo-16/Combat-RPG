package rpgcombat.balance.config;

/**
 * Configuració de l'anti-stall / sudden death.
 *
 * @param startTurn torn a partir del qual s'activa
 * @param increaseEveryTurns cada quants torns augmenta l'efecte
 * @param initialDamage dany inicial aplicat
 * @param damageIncreasePerStep increment de dany per interval
 */
public record AntiStallConfig(
    int startTurn,
    int increaseEveryTurns,
    double initialDamage,
    double damageIncreasePerStep
) {}