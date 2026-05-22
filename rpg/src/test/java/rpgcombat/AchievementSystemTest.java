package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import rpgcombat.achievements.AchievementEvent;
import rpgcombat.achievements.AchievementSystem;
import rpgcombat.achievements.AchievementUpdate;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;

class AchievementSystemTest {
    @Test
    void savesAccumulatedProgressWhenRoundStarts() throws Exception {
        Path tempDir = Files.createTempDirectory("achievement-system-save");
        Path savePath = tempDir.resolve("achievements.json");
        AchievementSystem system = AchievementSystem.load(
                List.of(countAchievement("ATTACK_ONCE", AchievementEvent.ACTION_ATTACK)),
                savePath.toString());

        system.onTurn(AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK));

        assertTrue(system.isCompleted("ATTACK_ONCE"));
        assertFalse(Files.exists(savePath));

        system.onRoundStart();

        assertTrue(Files.exists(savePath));
    }

    @Test
    void indexedUpdatesKeepEventAndAlwaysEvaluatedObjectivesWorking() throws Exception {
        Path tempDir = Files.createTempDirectory("achievement-system-index");
        AchievementSystem system = AchievementSystem.load(
                List.of(
                        countAchievement("ATTACK_ONCE", AchievementEvent.ACTION_ATTACK),
                        countAchievement("DEFEND_ONCE", AchievementEvent.ACTION_DEFEND),
                        new AchievementDefinition(
                                "SURVIVE_STATE",
                                "Survive state",
                                "",
                                1,
                                AchievementVisibility.visible(),
                                new AchievementObjective(
                                        AchievementObjectiveType.STATE_MAINTAINED,
                                        AchievementEvent.SURVIVE_TURN,
                                        null,
                                        null,
                                        List.of(),
                                        1.0,
                                        0.0))),
                tempDir.resolve("indexed-achievements.json").toString());

        system.onTurn(AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK));

        assertTrue(system.isCompleted("ATTACK_ONCE"));
        assertFalse(system.isCompleted("DEFEND_ONCE"));
        assertFalse(system.isCompleted("SURVIVE_STATE"));

        system.onTurn(AchievementUpdate.simple(null, AchievementEvent.SURVIVE_TURN));

        assertTrue(system.isCompleted("SURVIVE_STATE"));
    }

    private AchievementDefinition countAchievement(String id, AchievementEvent event) {
        return new AchievementDefinition(
                id,
                id,
                "",
                1,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.COUNT_EVENT,
                        event,
                        null,
                        null,
                        List.of(),
                        1.0,
                        0.0));
    }
}
