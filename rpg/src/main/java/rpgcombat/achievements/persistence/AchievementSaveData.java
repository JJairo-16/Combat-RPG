package rpgcombat.achievements.persistence;

import java.util.Map;

/** Arrel del fitxer de progrés d'assoliments. */
public record AchievementSaveData(
        int version,
        Map<String, AchievementSavedProgress> achievements) {
}
