package rpgcombat.perks.mission;

import java.util.List;

import rpgcombat.combat.models.Action;

/**
 * Representa la definició d'una missió carregada des de JSON.
 *
 * @param id identificador únic
 * @param name nom de la missió
 * @param description descripció
 * @param weight pes o probabilitat d'aparició
 * @param type tipus d'objectiu
 * @param event esdeveniment que la fa avançar
 * @param successEvent esdeveniment d'èxit
 * @param resetEvent esdeveniment de reinici
 * @param sequence seqüència d'accions requerida
 * @param target valor objectiu
 * @param value increment per esdeveniment
 * @param turns torns disponibles
 */
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