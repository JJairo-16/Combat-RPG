package rpgcombat.perks.config;

import java.util.List;

/** Configuració JSON d'una missió. */
public record MissionConfig(
        String id,
        String name,
        String description,
        Integer weight,
        ObjectiveConfig objective) {

    public record ObjectiveConfig(
            String type,
            String event,
            String successEvent,
            String resetEvent,
            List<String> sequence,
            Double target,
            Double value,
            Integer turns) {}
}
