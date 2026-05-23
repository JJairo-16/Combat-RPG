package rpgcombat.performance.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import rpgcombat.performance.PerformanceTestSupport;
import rpgcombat.performance.PerformanceTestSupport.TimedResult;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkFamily;
import rpgcombat.perks.PlayerPerkState;
import rpgcombat.perks.synergy.model.SynergyDefinition;
import rpgcombat.perks.synergy.model.SynergyType;
import rpgcombat.perks.synergy.runtime.SynergySystem;
import rpgcombat.weapons.passives.HitContext.Phase;

class PerkSynergyIndexesPerformanceTest {
    private static final int PERKS = 4_096;
    private static final int TAGS = 4_096;
    private static final int HOT_LOOKUPS = 30_000;
    private static final int HOT_WARMUP = 2_000;
    private static final int SYNERGY_PERKS = 1_024;
    private static final int SYNERGIES = 256;
    private static final int SYNERGY_LOOKUPS = 600;
    private static final int SYNERGY_WARMUP = 50;
    private static final String TARGET_PERK = "PERK_TARGET";
    private static final String TARGET_TAG = "TAG_TARGET";
    private static final Path REPORT_PATH = Path.of("target", "performance-reports", "perk-synergy-indexes.txt");

    @Test
    void perkAndSynergyIndexesBeatLinearScans(TestReporter reporter) throws IOException {
        PerkLookupScenario lookups = perkLookups();
        SynergyScenario synergies = synergyScenario();

        PerformanceTestSupport.repeat(() -> linearHasPerk(lookups.perks(), TARGET_PERK), HOT_WARMUP);
        PerformanceTestSupport.repeat(() -> bool(lookups.state().hasPerk(TARGET_PERK)), HOT_WARMUP);
        PerformanceTestSupport.repeat(() -> linearContainsTag(lookups.tags(), TARGET_TAG), HOT_WARMUP);
        PerformanceTestSupport.repeat(() -> bool(lookups.taggedPerk().tags().contains(TARGET_TAG)), HOT_WARMUP);
        PerformanceTestSupport.repeat(
                () -> legacyActiveSynergyCount(synergies.definitions(), synergies.state().perks()),
                SYNERGY_WARMUP);
        PerformanceTestSupport.repeat(() -> synergies.system().activeDisplayInfo(synergies.state()).size(), SYNERGY_WARMUP);

        TimedResult linearPerk =
                PerformanceTestSupport.time(() -> linearHasPerk(lookups.perks(), TARGET_PERK), HOT_LOOKUPS);
        TimedResult indexedPerk =
                PerformanceTestSupport.time(() -> bool(lookups.state().hasPerk(TARGET_PERK)), HOT_LOOKUPS);
        TimedResult linearTag =
                PerformanceTestSupport.time(() -> linearContainsTag(lookups.tags(), TARGET_TAG), HOT_LOOKUPS);
        TimedResult indexedTag =
                PerformanceTestSupport.time(() -> bool(lookups.taggedPerk().tags().contains(TARGET_TAG)), HOT_LOOKUPS);
        TimedResult linearSynergy = PerformanceTestSupport.time(
                () -> legacyActiveSynergyCount(synergies.definitions(), synergies.state().perks()),
                SYNERGY_LOOKUPS);
        TimedResult indexedSynergy = PerformanceTestSupport.time(
                () -> synergies.system().activeDisplayInfo(synergies.state()).size(),
                SYNERGY_LOOKUPS);

        assertEquals(HOT_LOOKUPS, linearPerk.result());
        assertEquals(HOT_LOOKUPS, indexedPerk.result());
        assertEquals(HOT_LOOKUPS, linearTag.result());
        assertEquals(HOT_LOOKUPS, indexedTag.result());
        assertEquals(SYNERGIES * SYNERGY_LOOKUPS, linearSynergy.result());
        assertEquals(SYNERGIES * SYNERGY_LOOKUPS, indexedSynergy.result());
        assertFaster("perk id", linearPerk, indexedPerk);
        assertFaster("perk tag", linearTag, indexedTag);
        assertFaster("synergy active scan", linearSynergy, indexedSynergy);

        String report = report(
                linearPerk,
                indexedPerk,
                linearTag,
                indexedTag,
                linearSynergy,
                indexedSynergy);
        reporter.publishEntry("perk-synergy-indexes-performance", report);
        PerformanceTestSupport.writeReport(REPORT_PATH, report);
    }

    private PerkLookupScenario perkLookups() {
        PlayerPerkState state = new PlayerPerkState();
        List<PerkDefinition> perks = new ArrayList<>(PERKS + 1);
        for (int i = 0; i < PERKS; i++) {
            PerkDefinition perk = perk("PERK_" + i, Set.of("COMMON"));
            state.addPerk(perk);
            perks.add(perk);
        }

        PerkDefinition target = perk(TARGET_PERK, Set.of("COMMON"));
        state.addPerk(target);
        perks.add(target);

        List<String> tags = new ArrayList<>(TAGS + 1);
        Set<String> tagSet = new LinkedHashSet<>();
        for (int i = 0; i < TAGS; i++) {
            String tag = "TAG_" + i;
            tags.add(tag);
            tagSet.add(tag);
        }
        tags.add(TARGET_TAG);
        tagSet.add(TARGET_TAG);
        return new PerkLookupScenario(state, List.copyOf(perks), List.copyOf(tags), perk("TAGGED", tagSet));
    }

    private SynergyScenario synergyScenario() {
        PlayerPerkState state = new PlayerPerkState();
        for (int i = 0; i < SYNERGY_PERKS; i++) {
            state.addPerk(perk("SYNERGY_PERK_" + i, Set.of(synergyTag(i))));
        }

        List<SynergyDefinition> definitions = new ArrayList<>(SYNERGIES);
        for (int i = 0; i < SYNERGIES; i++) {
            definitions.add(new SynergyDefinition(
                    "SYNERGY_" + i,
                    "Synergy " + i,
                    "",
                    SynergyType.ALTER_MEMBERS,
                    List.of(),
                    List.of(synergyTag(i)),
                    1,
                    null,
                    List.of()));
        }

        return new SynergyScenario(state, List.copyOf(definitions), new SynergySystem(definitions));
    }

    private int legacyActiveSynergyCount(List<SynergyDefinition> definitions, List<PerkDefinition> perks) {
        int active = 0;
        for (SynergyDefinition definition : definitions) {
            if (!linearRequiredPerksMet(definition, perks)) {
                continue;
            }
            if (linearMemberCount(definition, perks) >= definition.minMembers()) {
                active++;
            }
        }
        return active;
    }

    private boolean linearRequiredPerksMet(SynergyDefinition definition, List<PerkDefinition> perks) {
        for (String required : definition.requiredPerks()) {
            boolean found = false;
            for (PerkDefinition perk : perks) {
                if (required.equals(perk.id())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private int linearMemberCount(SynergyDefinition definition, List<PerkDefinition> perks) {
        int count = 0;
        if (definition.requiredTags().isEmpty()) {
            for (PerkDefinition perk : perks) {
                if (definition.requiredPerks().contains(perk.id())) {
                    count++;
                }
            }
            return count;
        }

        for (PerkDefinition perk : perks) {
            for (String tag : definition.requiredTags()) {
                if (perk.tags().contains(tag)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private int linearHasPerk(List<PerkDefinition> perks, String id) {
        for (PerkDefinition perk : perks) {
            if (id.equals(perk.id())) {
                return 1;
            }
        }
        return 0;
    }

    private int linearContainsTag(List<String> tags, String expected) {
        for (String tag : tags) {
            if (expected.equals(tag)) {
                return 1;
            }
        }
        return 0;
    }

    private PerkDefinition perk(String id, Set<String> tags) {
        return new PerkDefinition(
                id,
                id,
                "",
                PerkFamily.STRATEGY,
                Phase.AFTER_HIT,
                1,
                List.of(),
                List.of(),
                tags);
    }

    private String synergyTag(int index) {
        return "SYNERGY_TAG_" + index;
    }

    private int bool(boolean value) {
        return value ? 1 : 0;
    }

    private void assertFaster(String label, TimedResult linear, TimedResult indexed) {
        assertTrue(indexed.wallNanos() < linear.wallNanos(),
                label + " linear=" + PerformanceTestSupport.compactMetrics(linear)
                        + " indexed=" + PerformanceTestSupport.compactMetrics(indexed));
    }

    private String report(
            TimedResult linearPerk,
            TimedResult indexedPerk,
            TimedResult linearTag,
            TimedResult indexedTag,
            TimedResult linearSynergy,
            TimedResult indexedSynergy) {
        return String.join(System.lineSeparator(),
                "Perk and synergy indexes performance",
                "========================================",
                "Perk id items       : " + PerformanceTestSupport.integerText(PERKS + 1),
                "Perk tags           : " + PerformanceTestSupport.integerText(TAGS + 1),
                "Hot lookups         : " + PerformanceTestSupport.integerText(HOT_LOOKUPS),
                "Synergy perks       : " + PerformanceTestSupport.integerText(SYNERGY_PERKS),
                "Synergy definitions : " + PerformanceTestSupport.integerText(SYNERGIES),
                "Synergy lookups     : " + PerformanceTestSupport.integerText(SYNERGY_LOOKUPS),
                "",
                "Note: ramDelta is an approximate heap delta before/after each measured loop.",
                "",
                section("PERK ID LOOKUP", linearPerk, indexedPerk),
                "",
                section("PERK TAG LOOKUP", linearTag, indexedTag),
                "",
                section("ACTIVE SYNERGY LOOKUP", linearSynergy, indexedSynergy),
                "========================================");
    }

    private String section(String title, TimedResult linear, TimedResult indexed) {
        return String.join(System.lineSeparator(),
                title,
                "  Linear scan",
                PerformanceTestSupport.metrics(linear),
                "",
                "  Indexed lookup",
                PerformanceTestSupport.metrics(indexed),
                "",
                "  Speedup",
                "    wall : " + PerformanceTestSupport.speedupText(linear.wallNanos(), indexed.wallNanos()));
    }

    private record PerkLookupScenario(
            PlayerPerkState state,
            List<PerkDefinition> perks,
            List<String> tags,
            PerkDefinition taggedPerk) {
    }

    private record SynergyScenario(
            PlayerPerkState state,
            List<SynergyDefinition> definitions,
            SynergySystem system) {
    }
}
