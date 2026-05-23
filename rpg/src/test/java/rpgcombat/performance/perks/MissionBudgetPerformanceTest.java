package rpgcombat.performance.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import rpgcombat.combat.models.Action;
import rpgcombat.performance.PerformanceTestSupport;
import rpgcombat.performance.PerformanceTestSupport.TimedResult;
import rpgcombat.perks.mission.MissionBudget;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionEvent;
import rpgcombat.perks.mission.MissionProgress;
import rpgcombat.perks.mission.MissionUpdate;
import rpgcombat.perks.mission.ObjectiveType;

class MissionBudgetPerformanceTest {
    private static final int MISSIONS = 4_096;
    private static final int LOOKUPS = 30_000;
    private static final int WARMUP = 2_000;
    private static final Path REPORT_PATH = Path.of("target", "performance-reports", "mission-budget.txt");

    @Test
    void indexedCandidatesBeatLinearMissionScans(TestReporter reporter) throws IOException {
        List<MissionProgress> progress = progressItems();
        MissionBudget budget = new MissionBudget(progress);
        MissionUpdate update = update(MissionEvent.ACTION_ATTACK);

        PerformanceTestSupport.repeat(() -> linearCandidates(progress, update).size(), WARMUP);
        PerformanceTestSupport.repeat(() -> budget.candidates(update).size(), WARMUP);

        TimedResult linear = PerformanceTestSupport.time(() -> linearCandidates(progress, update).size(), LOOKUPS);
        TimedResult indexed = PerformanceTestSupport.time(() -> budget.candidates(update).size(), LOOKUPS);

        assertEquals(LOOKUPS, linear.result());
        assertEquals(LOOKUPS, indexed.result());
        assertTrue(indexed.wallNanos() < linear.wallNanos(), summary(linear, indexed));

        String report = report(linear, indexed);
        reporter.publishEntry("mission-budget-performance", report);
        PerformanceTestSupport.writeReport(REPORT_PATH, report);
    }

    private List<MissionProgress> progressItems() {
        List<MissionProgress> progress = new ArrayList<>(MISSIONS + 1);
        for (int i = 0; i < MISSIONS; i++) {
            MissionDefinition definition = countMission(
                    "irrelevant-" + i,
                    i % 2 == 0 ? MissionEvent.ACTION_DEFEND : MissionEvent.ACTION_ATTACK);
            MissionProgress mission = new MissionProgress(definition);
            if (i % 2 != 0) {
                mission.update(update(MissionEvent.ACTION_ATTACK));
            }
            progress.add(mission);
        }
        progress.add(new MissionProgress(countMission("attack-target", MissionEvent.ACTION_ATTACK)));
        return List.copyOf(progress);
    }

    private List<MissionProgress> linearCandidates(List<MissionProgress> missions, MissionUpdate update) {
        List<MissionProgress> result = new ArrayList<>();
        for (MissionProgress mission : missions) {
            if (observes(mission, update)) {
                result.add(mission);
            }
        }
        return List.copyOf(result);
    }

    private boolean observes(MissionProgress mission, MissionUpdate update) {
        if (mission == null || mission.completed() || mission.rewardClaimed() || mission.definition() == null) {
            return false;
        }

        MissionDefinition definition = mission.definition();
        ObjectiveType type = definition.type();
        if (type == null || type == ObjectiveType.AVOID_EVENT_FOR_TURNS || type == ObjectiveType.STATE_MAINTAINED) {
            return true;
        }
        if (type == ObjectiveType.ACTION_SEQUENCE) {
            return update != null && update.ownerAction() != null;
        }
        if (type == ObjectiveType.CONSECUTIVE_EVENT) {
            return update != null
                    && (update.has(definition.successEvent()) || update.has(definition.resetEvent()));
        }
        return definition.event() == null || update != null && update.has(definition.event());
    }

    private MissionDefinition countMission(String id, MissionEvent event) {
        return new MissionDefinition(
                id,
                id,
                "",
                1,
                ObjectiveType.COUNT_EVENT,
                event,
                null,
                null,
                List.of(Action.ATTACK),
                1,
                0,
                0);
    }

    private MissionUpdate update(MissionEvent event) {
        Action action = event == MissionEvent.ACTION_ATTACK ? Action.ATTACK : Action.DEFEND;
        return new MissionUpdate(null, null, action, null, null, 0, Set.of(event));
    }

    private String summary(TimedResult linear, TimedResult indexed) {
        return "MissionBudget perf linear=" + PerformanceTestSupport.compactMetrics(linear)
                + " indexed=" + PerformanceTestSupport.compactMetrics(indexed);
    }

    private String report(TimedResult linear, TimedResult indexed) {
        return String.join(System.lineSeparator(),
                "MissionBudget performance",
                "========================================",
                "Missions   : " + PerformanceTestSupport.integerText(MISSIONS + 1),
                "Lookups    : " + PerformanceTestSupport.integerText(LOOKUPS),
                "Candidates : 1 per lookup",
                "",
                "Note: ramDelta is an approximate heap delta before/after each measured loop.",
                "",
                "CANDIDATE LOOKUP",
                "  Linear scan",
                PerformanceTestSupport.metrics(linear),
                "",
                "  Indexed lookup",
                PerformanceTestSupport.metrics(indexed),
                "",
                "  Speedup",
                "    wall : " + PerformanceTestSupport.speedupText(linear.wallNanos(), indexed.wallNanos()),
                "========================================");
    }
}
