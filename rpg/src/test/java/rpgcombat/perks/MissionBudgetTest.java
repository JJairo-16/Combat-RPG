package rpgcombat.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import rpgcombat.combat.models.Action;
import rpgcombat.perks.mission.MissionBudget;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionEvent;
import rpgcombat.perks.mission.MissionProgress;
import rpgcombat.perks.mission.MissionUpdate;
import rpgcombat.perks.mission.ObjectiveType;

class MissionBudgetTest {
    @Test
    void indexesPendingMissionsByEventsAndKeepsAlwaysObservedObjectives() {
        MissionProgress completedAttack = new MissionProgress(countMission("ATTACK", MissionEvent.ACTION_ATTACK));
        completedAttack.update(update(MissionEvent.ACTION_ATTACK));

        MissionProgress defend = new MissionProgress(countMission("DEFEND", MissionEvent.ACTION_DEFEND));
        MissionProgress sequence = new MissionProgress(sequenceMission("SEQUENCE"));
        MissionProgress avoid = new MissionProgress(avoidMission("AVOID"));
        MissionBudget budget = new MissionBudget(List.of(completedAttack, defend, sequence, avoid));

        assertEquals(List.of(avoid, sequence), budget.candidates(update(MissionEvent.ACTION_ATTACK)));
        assertEquals(List.of(avoid, defend, sequence), budget.candidates(update(MissionEvent.ACTION_DEFEND)));
    }

    @Test
    void returnsImmutableSnapshotsAndInvalidatesThemAfterRemoval() {
        MissionProgress attack = new MissionProgress(countMission("ATTACK", MissionEvent.ACTION_ATTACK));
        MissionBudget budget = new MissionBudget(List.of(attack));
        MissionUpdate attackUpdate = update(MissionEvent.ACTION_ATTACK);

        List<MissionProgress> candidates = budget.candidates(attackUpdate);
        assertEquals(List.of(attack), candidates);
        assertThrows(UnsupportedOperationException.class, candidates::clear);

        budget.remove(attack);

        assertEquals(List.of(attack), candidates);
        assertEquals(List.of(), budget.candidates(attackUpdate));
    }

    private MissionDefinition countMission(String id, MissionEvent event) {
        return new MissionDefinition(id, id, "", 1, ObjectiveType.COUNT_EVENT, event, null, null, List.of(), 1, 0, 0);
    }

    private MissionDefinition sequenceMission(String id) {
        return new MissionDefinition(
                id,
                id,
                "",
                1,
                ObjectiveType.ACTION_SEQUENCE,
                null,
                null,
                null,
                List.of(Action.ATTACK, Action.DEFEND),
                2,
                0,
                0);
    }

    private MissionDefinition avoidMission(String id) {
        return new MissionDefinition(
                id,
                id,
                "",
                1,
                ObjectiveType.AVOID_EVENT_FOR_TURNS,
                MissionEvent.SELF_HIT,
                null,
                null,
                List.of(),
                2,
                0,
                0);
    }

    private MissionUpdate update(MissionEvent event) {
        return new MissionUpdate(null, null, null, null, null, 0, Set.of(event));
    }
}
