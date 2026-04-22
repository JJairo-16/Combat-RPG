package rpgcombat.balance.config;

/**
 * Configuració de l'atac carregat.
 */
public record ChargedAttackConfig(
    double damageMultiplier,
    boolean canStoreOnlyOneCharge,
    boolean consumedOnNextAttack,
    int staggerTurnsOnHit
) {}