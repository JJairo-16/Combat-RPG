package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import rpgcombat.achievements.AchievementEvent;
import rpgcombat.achievements.AchievementProgress;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;
import rpgcombat.achievements.persistence.AchievementStore;

class AchievementStoreTest {
    @Test
    void completedAchievementsPersistOnlyCompletionData() throws Exception {
        Path tempDir = Files.createTempDirectory("achievement-store-completed");
        AchievementDefinition definition = damageAchievement("DONE", 5.0);
        AchievementProgress completed = new AchievementProgress(
                definition,
                5.0,
                2,
                true,
                Instant.parse("2026-05-08T15:34:58.125884Z"),
                Map.of("value", 5.0),
                Map.of("actor", 2));
        Path savePath = tempDir.resolve("completed.json");
        AchievementStore store = new AchievementStore();

        store.save(savePath, List.of(completed));

        String json = Files.readString(savePath);
        assertTrue(json.contains("\"completed\": true"));
        assertTrue(json.contains("\"completedAt\": \"2026-05-08T15:34:58.125884Z\""));
        assertFalse(json.contains("\"progress\""));
        assertFalse(json.contains("\"sequenceIndex\""));
        assertFalse(json.contains("\"valueProgress\""));
        assertFalse(json.contains("\"actorSequenceProgress\""));

        AchievementProgress loaded = store.load(savePath, List.of(definition)).get("DONE");
        assertTrue(loaded.completed());
        assertEquals(5, loaded.viewProgress());
    }

    @Test
    void pendingAchievementDecimalsAreRoundedBeforeSaving() throws Exception {
        Path tempDir = Files.createTempDirectory("achievement-store-pending");
        AchievementDefinition definition = damageAchievement("PENDING", 10.0);
        AchievementProgress pending = new AchievementProgress(
                definition,
                1.236,
                0,
                false,
                null,
                Map.of("damage", 2.345),
                Map.of());
        Path savePath = tempDir.resolve("pending.json");

        new AchievementStore().save(savePath, List.of(pending));

        String json = Files.readString(savePath);
        assertTrue(json.contains("\"progress\": 1.24"));
        assertTrue(json.contains("\"damage\": 2.35"));
        assertFalse(json.contains("1.236"));
        assertFalse(json.contains("2.345"));
    }

    private AchievementDefinition damageAchievement(String id, double target) {
        return new AchievementDefinition(
                id,
                id,
                "",
                (int) target,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.SUM_VALUE,
                        AchievementEvent.DAMAGE_DEALT,
                        null,
                        null,
                        List.of(),
                        target,
                        0.0));
    }
}
