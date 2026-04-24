package rpgcombat.balance.config.character;

/**
 * Configuració del pacte de sang.
 *
 * @param manaThreshold llindar de mana per activar l'efecte
 * @param baseHpCostPercent percentatge base de vida consumida
 * @param wisdomReduction reducció del cost segons saviesa
 * @param minHpCostPercent percentatge mínim de vida consumida
 */
public record BloodPactConfig(
    double manaThreshold,
    double baseHpCostPercent,
    double wisdomReduction,
    double minHpCostPercent
) {
    
}