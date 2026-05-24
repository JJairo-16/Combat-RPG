package rpgcombat.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import rpgcombat.perks.synergy.model.SynergyDefinition;
import rpgcombat.perks.synergy.model.SynergyType;
import rpgcombat.perks.synergy.runtime.SynergySystem;
import rpgcombat.perks.synergy.view.SynergyDisplayInfo;
import rpgcombat.weapons.passives.HitContext.Phase;

class SynergySystemTest {
    @Test
    void perkTagsAreIndexedSetsAndTagSynergiesSeeEachPerkOnce() {
        PerkDefinition first = perk("FIRST", " fire ");
        PerkDefinition second = perk("SECOND", "FIRE");
        PlayerPerkState state = new PlayerPerkState();
        state.addPerk(first);
        state.addPerk(first);
        state.addPerk(second);

        SynergySystem synergies = new SynergySystem(List.of(new SynergyDefinition(
                "FIRE_PAIR",
                "Fire pair",
                "",
                SynergyType.ALTER_MEMBERS,
                List.of(),
                List.of("FIRE"),
                2,
                null,
                List.of())));

        assertEquals(Set.of("FIRE"), first.tags());
        Set<String> tags = first.tags();
        assertThrows(UnsupportedOperationException.class, tags::clear);
        assertTrue(state.hasPerk("FIRST"));
        assertEquals(2, state.perks().size());
        assertEquals(List.of("FIRE_PAIR"), synergies.activeDisplayInfo(state).stream()
                .map(SynergyDisplayInfo::id)
                .toList());
    }

    @Test
    void activeDisplayCacheInvalidatesWhenPerksChange() {
        PlayerPerkState state = new PlayerPerkState();
        state.addPerk(perk("FIRE", "FIRE"));
        SynergySystem synergies = new SynergySystem(List.of(
                tagSynergy("FIRE_SOLO", "FIRE"),
                tagSynergy("ICE_SOLO", "ICE")));

        assertEquals(List.of("FIRE_SOLO"), activeIds(synergies, state));
        assertEquals(List.of("FIRE_SOLO"), activeIds(synergies, state));

        state.addPerk(perk("ICE", "ICE"));

        assertEquals(List.of("FIRE_SOLO", "ICE_SOLO"), activeIds(synergies, state));
    }

    private PerkDefinition perk(String id, String tag) {
        return new PerkDefinition(
                id,
                id,
                "",
                PerkFamily.STRATEGY,
                Phase.AFTER_HIT,
                1,
                List.of(),
                List.of(),
                Set.of(tag));
    }

    private SynergyDefinition tagSynergy(String id, String tag) {
        return new SynergyDefinition(
                id,
                id,
                "",
                SynergyType.ALTER_MEMBERS,
                List.of(),
                List.of(tag),
                1,
                null,
                List.of());
    }

    private List<String> activeIds(SynergySystem synergies, PlayerPerkState state) {
        return synergies.activeDisplayInfo(state).stream()
                .map(SynergyDisplayInfo::id)
                .toList();
    }
}
