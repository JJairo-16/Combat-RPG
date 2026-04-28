package rpgcombat.perks.mission;

import java.util.List;
import java.util.Random;

/**
 * Manté el registre global de missions disponibles i permet seleccionar-ne una.
 */
public final class MissionRegistry {
    private static List<MissionDefinition> missions = List.of();

    private MissionRegistry() {}

    /**
     * Inicialitza el registre amb les missions carregades.
     *
     * @param loaded llista de missions
     */
    public static void initialize(List<MissionDefinition> loaded) {
        missions = loaded == null ? List.of() : List.copyOf(loaded);
    }

    /**
     * Selecciona una missió aleatòria segons el seu pes.
     *
     * @param rng generador aleatori
     * @return missió seleccionada o null si no n'hi ha
     */
    public static MissionDefinition roll(Random rng) {
        if (missions.isEmpty()) return null;
        int total = missions.stream().mapToInt(MissionDefinition::weight).sum();
        int roll = rng.nextInt(Math.max(1, total));
        int acc = 0;
        for (MissionDefinition mission : missions) {
            acc += mission.weight();
            if (roll < acc) return mission;
        }
        return missions.get(missions.size() - 1);
    }
}