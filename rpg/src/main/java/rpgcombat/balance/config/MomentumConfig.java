package rpgcombat.balance.config;

/**
 * Configuració de momentum.
 *
 * @param maxStacks màxim d'acumulacions
 * @param attackBonusPerStack bonus d'atac per acumulació
 * @param dodgeBonusPerStack bonus d'esquiva per acumulació
 * @param suppressionWhenTargetLowHealth reducció quan l'objectiu té poca vida
 * @param suppressionHealthOffset offset de vida per aplicar la reducció
 * @param gain configuració de guany de momentum
 * @param loss configuració de pèrdua de momentum
 * @param noDecayWhileDesperateCharging evita la pèrdua mentre es carrega en estat desesperat
 */
public record MomentumConfig(
    int maxStacks,
    double attackBonusPerStack,
    double dodgeBonusPerStack,
    double suppressionWhenTargetLowHealth,
    double suppressionHealthOffset,
    GainConfig gain,
    LossConfig loss,
    boolean noDecayWhileDesperateCharging
) {

    /**
     * Configuració de guany de momentum.
     *
     * @param onSuccessfulHit acumulacions guanyades en impactar
     * @param onSuccessfulDodge acumulacions guanyades en esquivar
     * @param underdogExtraOnSuccessfulDodge extra en esquiva si és underdog
     */
    public record GainConfig(
        int onSuccessfulHit,
        int onSuccessfulDodge,
        int underdogExtraOnSuccessfulDodge
    ) {}

    /**
     * Configuració de pèrdua de momentum.
     *
     * @param onBeingHit acumulacions perdudes en rebre cop
     * @param onFailedAttack acumulacions perdudes en fallar atac
     * @param onAttackDodged acumulacions perdudes si l'atac és esquivat
     * @param onPassiveTurnDefend acumulacions perdudes en torn passiu defensant
     * @param onPassiveTurnCharge acumulacions perdudes en torn passiu carregant
     */
    public record LossConfig(
        int onBeingHit,
        int onFailedAttack,
        int onAttackDodged,
        int onPassiveTurnDefend,
        int onPassiveTurnCharge
    ) {}
}