package rpgcombat.perks.mission;

import java.util.List;

import rpgcombat.combat.models.Action;

/** Definició lleugera d'una missió carregada des de JSON. */
public record MissionDefinition(
        String id,
        String name,
        String description,
        int weight,
        ObjectiveType type,
        MissionEvent event,
        MissionEvent successEvent,
        MissionEvent resetEvent,
        List<Action> sequence,
        double target,
        double value,
        int turns) {
}
