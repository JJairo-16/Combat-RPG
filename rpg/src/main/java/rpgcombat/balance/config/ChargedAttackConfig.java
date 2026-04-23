package rpgcombat.balance.config;

/**
 * Configuració de l'atac carregat.
 *
 * @param damageMultiplier multiplicador de dany de l'atac carregat
 * @param canStoreOnlyOneCharge indica si només es pot emmagatzemar una càrrega
 * @param consumedOnNextAttack indica si es consumeix en el següent atac
 * @param staggerTurnsOnHit torns d'aturdiment en impactar
 */
public record ChargedAttackConfig(
    double damageMultiplier,
    boolean canStoreOnlyOneCharge,
    boolean consumedOnNextAttack,
    int staggerTurnsOnHit
) {}