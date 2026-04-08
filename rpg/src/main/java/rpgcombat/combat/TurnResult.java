package rpgcombat.combat;

import java.util.List;

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