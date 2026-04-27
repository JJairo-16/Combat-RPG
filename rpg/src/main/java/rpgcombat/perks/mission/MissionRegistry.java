package rpgcombat.perks.mission;

import java.util.List;
import java.util.Random;

/** Registre estàtic de missions disponibles. */
public final class MissionRegistry {
    private static List<MissionDefinition> missions = List.of();

    private MissionRegistry() {}

    public static void initialize(List<MissionDefinition> loaded) {
        missions = loaded == null ? List.of() : List.copyOf(loaded);
    }

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
