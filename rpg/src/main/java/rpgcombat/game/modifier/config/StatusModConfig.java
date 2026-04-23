package rpgcombat.game.modifier.config;

/**
 * Configuració d'un modificador d'estat.
 *
 * @param priority prioritat d'aplicació
 * @param minCharges càrregues mínimes
 * @param maxCharges càrregues màximes
 * @param minStacks acumulacions mínimes
 * @param maxStacks acumulacions màximes
 * @param minRemainingTurns torns mínims restants
 * @param maxRemainingTurns torns màxims restants
 * @param label etiqueta descriptiva
 * @param actionKey clau de l'acció associada
 */
public record StatusModConfig(
    int priority,
    Integer minCharges,
    Integer maxCharges,
    Integer minStacks,
    Integer maxStacks,
    Integer minRemainingTurns,
    Integer maxRemainingTurns,
    String label,
    String actionKey
) {}