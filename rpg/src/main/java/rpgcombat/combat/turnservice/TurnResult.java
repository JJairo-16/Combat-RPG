package rpgcombat.combat.turnservice;

import java.util.List;

/**
 * Representa el resultat complet d’un torn de combat.
 *
 * @param actorName nom de l’atacant
 * @param attackerMessage missatge de l’acció d’atac
 * @param startMessages missatges d’inici de torn
 * @param preDefenseMessages missatges abans de la defensa
 * @param defenseMessage resultat de la defensa
 * @param postDefenseMessages missatges després de la defensa
 * @param endTurnMessages missatges de final de torn
 * @param damageDealt dany infligit final
 * @param critical indica si ha estat cop crític
 */
public record TurnResult(
        String actorName,
        String attackerMessage,
        List<String> startMessages,
        List<String> preDefenseMessages,
        String defenseMessage,
        List<String> postDefenseMessages,
        List<String> endTurnMessages,
        double damageDealt,
        boolean critical) {
}