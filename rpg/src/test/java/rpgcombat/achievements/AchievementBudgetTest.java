package rpgcombat.achievements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.achievements.config.AchievementVisibility;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;

class AchievementBudgetTest {
    @Test
    void skipsCompletedProgressAndIndexesActionSequences() {
        AchievementProgress completedAttack = new AchievementProgress(
                countAchievement("ATTACK_ONCE", AchievementEvent.ACTION_ATTACK));
        completedAttack.update(AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK));

        AchievementProgress defend = new AchievementProgress(
                countAchievement("DEFEND_ONCE", AchievementEvent.ACTION_DEFEND));
        AchievementProgress sequence = new AchievementProgress(new AchievementDefinition(
                "ACTION_SEQUENCE",
                "Action sequence",
                "",
                1,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.ACTION_SEQUENCE,
                        null,
                        null,
                        null,
                        List.of(Action.ATTACK, Action.DEFEND),
                        2.0,
                        0.0)));

        AchievementBudget budget = new AchievementBudget(List.of(completedAttack, defend, sequence));

        assertEquals(List.of(sequence),
                budget.candidates(AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK)));
        assertEquals(List.of(defend, sequence),
                budget.candidates(AchievementUpdate.simple(null, AchievementEvent.ACTION_DEFEND)));
    }

    @Test
    void invalidatesIndexedSnapshotsWhenProgressLeavesTheBudget() {
        AchievementProgress attack = new AchievementProgress(
                countAchievement("ATTACK_ONCE", AchievementEvent.ACTION_ATTACK));
        AchievementBudget budget = new AchievementBudget(List.of(attack));
        AchievementUpdate attackUpdate = AchievementUpdate.simple(null, AchievementEvent.ACTION_ATTACK);

        assertEquals(List.of(attack), budget.candidates(attackUpdate));

        budget.remove(attack);

        assertEquals(List.of(), budget.candidates(attackUpdate));
    }

    @Test
    void exposesImmutableCandidateSnapshotsWithoutDuplicatesAcrossEventBuckets() {
        AchievementProgress attack = new AchievementProgress(
                countAchievement("ATTACK_ONCE", AchievementEvent.ACTION_ATTACK));
        AchievementProgress sequence = new AchievementProgress(actionSequenceAchievement("ATTACK_DEFEND"));
        AchievementBudget budget = new AchievementBudget(List.of(attack, sequence));
        AchievementUpdate multiActionUpdate = new AchievementUpdate(
                null,
                null,
                null,
                null,
                null,
                Winner.NONE,
                0,
                new LinkedHashSet<>(List.of(
                        AchievementEvent.ACTION_ATTACK,
                        AchievementEvent.ACTION_DEFEND)),
                Map.of());

        List<AchievementProgress> candidates = budget.candidates(multiActionUpdate);

        assertEquals(List.of(attack, sequence), candidates);
        assertThrows(UnsupportedOperationException.class, candidates::clear);

        budget.remove(attack);
        budget.rebuild(List.of(attack));

        assertEquals(List.of(attack, sequence), candidates);
        assertEquals(List.of(attack), budget.candidates(multiActionUpdate));
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

    private AchievementDefinition actionSequenceAchievement(String id) {
        return new AchievementDefinition(
                id,
                id,
                "",
                1,
                AchievementVisibility.visible(),
                new AchievementObjective(
                        AchievementObjectiveType.ACTION_SEQUENCE,
                        null,
                        null,
                        null,
                        List.of(Action.ATTACK, Action.DEFEND),
                        2.0,
                        0.0));
    }
}
