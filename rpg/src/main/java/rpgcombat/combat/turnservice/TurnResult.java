package rpgcombat.combat.turnservice;

import java.util.List;

import rpgcombat.combat.ui.messages.CombatMessage;

/**
 * Representa el resultat complet d'un torn de combat.
 */
public record TurnResult(
        String actorName,
        String attackerMessage,
        List<CombatMessage> startMessages,
        List<CombatMessage> preDefenseMessages,
        String defenseMessage,
        List<CombatMessage> postDefenseMessages,
        List<CombatMessage> endTurnMessages,
        double damageDealt,
        boolean critical,
        boolean selfHit,
        boolean chargedHit) {

    public TurnResult(
            String actorName,
            String attackerMessage,
            List<CombatMessage> startMessages,
            List<CombatMessage> preDefenseMessages,
            String defenseMessage,
            List<CombatMessage> postDefenseMessages,
            List<CombatMessage> endTurnMessages,
            double damageDealt,
            boolean critical) {
        this(actorName, attackerMessage, startMessages, preDefenseMessages, defenseMessage, postDefenseMessages,
                endTurnMessages, damageDealt, critical, false, false);
    }
}
