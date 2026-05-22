package rpgcombat.perks.synergy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PlayerPerkState;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.perks.effect.SynergyBonusEffect;

/** Calcula, previsualitza i aplica sinergies de perks. */
public final class SynergySystem {
    private final List<SynergyDefinition> definitions;

    /** Inicialitza el sistema amb les sinergies disponibles. */
    public SynergySystem(List<SynergyDefinition> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    /** Previsualitza les sinergies que activaria una perk candidata. */
    public SynergyPreview preview(PlayerPerkState state, PerkDefinition candidate) {
        if (state == null || candidate == null)
            return SynergyPreview.empty();

        List<PerkDefinition> before = state.perks();
        List<PerkDefinition> after = new ArrayList<>(before);
        if (after.stream().noneMatch(perk -> perk.id().equals(candidate.id()))) {
            after.add(candidate);
        }

        Map<String, ActiveSynergy> beforeMap = activeMap(before);
        Map<String, ActiveSynergy> afterMap = activeMap(after);

        List<String> activated = new ArrayList<>();
        List<String> upgraded = new ArrayList<>();

        for (ActiveSynergy next : afterMap.values()) {
            ActiveSynergy previous = beforeMap.get(next.definition().id());
            if (previous == null) {
                activated.add(next.definition().name());
            } else if (next.rank() > previous.rank()) {
                upgraded.add(next.definition().name());
            }
        }

        return new SynergyPreview(!activated.isEmpty() || !upgraded.isEmpty(), activated, upgraded);
    }

    /** Previsualitza sinergies per a diverses perks candidates. */
    public Map<String, SynergyPreview> previewAll(PlayerPerkState state, List<PerkDefinition> candidates) {
        if (candidates == null || candidates.isEmpty())
            return Map.of();
        Map<String, SynergyPreview> result = new HashMap<>();
        for (PerkDefinition candidate : candidates) {
            if (candidate != null) {
                result.put(candidate.id(), preview(state, candidate));
            }
        }
        return result;
    }

    /** Recalcula i aplica els efectes de sinergia al jugador. */
    public void refresh(Character player, PlayerPerkState state) {
        if (player == null || state == null)
            return;

        removeSynergyBonuses(player);

        List<PerkDefinition> perks = state.perks();
        Map<String, List<MemberAlteration>> alterationsByPerk = activeAlterationsByPerk(perks);
        Map<String, List<String>> descriptionsByPerk = activeAlterationDescriptions(perks);
        List<ActiveSynergy> active = activeList(perks);

        for (PerkDefinition perk : perks) {
            player.removeEffect(PerkEffectFactory.keyFor(perk));
            List<MemberAlteration> alterations = alterationsByPerk.getOrDefault(perk.id(), List.of());
            player.addEffect(PerkEffectFactory.createAugmented(perk, alterations));
        }

        for (ActiveSynergy activeSynergy : active) {
            if (activeSynergy.definition().type() == SynergyType.BONUS_EXTRA && activeSynergy.level() != null) {
                Effect effect = PerkEffectFactory.createSynergyBonus(activeSynergy.definition(), activeSynergy.level());
                player.removeEffect(effect.key());
                player.addEffect(effect);
            }
        }

        state.setSynergyDescriptions(descriptionsByPerk);
        state.setActiveSynergyIds(active.stream()
                .map(a -> a.definition().id())
                .distinct()
                .toList());
        state.setActiveSynergyNames(active.stream()
                .map(a -> a.definition().name())
                .distinct()
                .toList());
        Map<String, String> namesById = new HashMap<>();
        for (ActiveSynergy activeSynergy : active) {
            namesById.put(activeSynergy.definition().id(), activeSynergy.definition().name());
        }
        state.setActiveSynergyNamesById(namesById);
    }

    /** Retorna la informació de les sinergies actives per mostrar-la. */
    public List<SynergyDisplayInfo> activeDisplayInfo(PlayerPerkState state) {
        if (state == null)
            return List.of();

        return activeList(state.perks()).stream()
                .map(active -> new SynergyDisplayInfo(
                        active.definition().id(),
                        active.definition().name(),
                        active.definition().description(),
                        active.definition().type(),
                        active.members(),
                        active.rank()))
                .toList();
    }

    /** Elimina els efectes extra de sinergia del jugador. */
    private void removeSynergyBonuses(Character player) {
        for (SynergyBonusEffect effect : player.effectsOfType(SynergyBonusEffect.class)) {
            player.removeEffect(effect.key());
        }
    }

    /** Agrupa les alteracions actives per perk afectada. */
    private Map<String, List<MemberAlteration>> activeAlterationsByPerk(List<PerkDefinition> perks) {
        Map<String, List<MemberAlteration>> result = new HashMap<>();
        Set<String> perkIds = perkIds(perks);

        for (ActiveSynergy active : activeList(perks)) {
            if (active.definition().type() != SynergyType.ALTER_MEMBERS)
                continue;
            for (MemberAlteration alteration : active.definition().alterations()) {
                if (perkIds.contains(alteration.memberPerkId())) {
                    result.computeIfAbsent(alteration.memberPerkId(), ignored -> new ArrayList<>()).add(alteration);
                }
            }
        }
        return result;
    }

    /** Agrupa les descripcions d’alteracions actives per perk. */
    private Map<String, List<String>> activeAlterationDescriptions(List<PerkDefinition> perks) {
        Map<String, List<String>> result = new HashMap<>();
        Set<String> perkIds = perkIds(perks);

        for (ActiveSynergy active : activeList(perks)) {
            if (active.definition().type() != SynergyType.ALTER_MEMBERS)
                continue;
            for (MemberAlteration alteration : active.definition().alterations()) {
                if (!perkIds.contains(alteration.memberPerkId()))
                    continue;
                String text = alteration.descriptionAppend().isBlank()
                        ? active.definition().description()
                        : alteration.descriptionAppend();
                if (!text.isBlank()) {
                    result.computeIfAbsent(alteration.memberPerkId(), ignored -> new ArrayList<>())
                            .add(active.definition().name() + ": " + text);
                }
            }
        }
        return result;
    }

    /** Retorna les sinergies actives indexades per identificador. */
    private Map<String, ActiveSynergy> activeMap(List<PerkDefinition> perks) {
        Map<String, ActiveSynergy> result = new HashMap<>();
        for (ActiveSynergy active : activeList(perks)) {
            result.put(active.definition().id(), active);
        }
        return result;
    }

    /** Calcula la llista de sinergies actives. */
    private List<ActiveSynergy> activeList(List<PerkDefinition> perks) {
        if (definitions.isEmpty() || perks == null || perks.isEmpty())
            return List.of();
        List<ActiveSynergy> result = new ArrayList<>();

        for (SynergyDefinition definition : definitions) {
            int members = memberCount(definition, perks);
            if (!requiredPerksMet(definition, perks) || members < definition.minMembers())
                continue;

            SynergyLevel level = null;
            int rank = members;
            if (definition.type() == SynergyType.BONUS_EXTRA) {
                level = bestLevel(definition, members);
                if (level == null)
                    continue;
                rank = level.members();
            }

            result.add(new ActiveSynergy(definition, members, rank, level));
        }

        result.sort(Comparator.comparing(a -> a.definition().id()));
        return List.copyOf(result);
    }

    /** Selecciona el millor nivell disponible per nombre de membres. */
    private SynergyLevel bestLevel(SynergyDefinition definition, int members) {
        if (definition.scaling() == null || definition.scaling().levels().isEmpty())
            return null;
        return definition.scaling().levels().stream()
                .filter(level -> level.members() <= members)
                .max(Comparator.comparingInt(SynergyLevel::members))
                .orElse(null);
    }

    /** Comprova si les perks requerides són presents. */
    private boolean requiredPerksMet(SynergyDefinition definition, List<PerkDefinition> perks) {
        Set<String> ids = perkIds(perks);
        return ids.containsAll(definition.requiredPerks());
    }

    /** Compta els membres que compleixen els requisits de la sinergia. */
    private int memberCount(SynergyDefinition definition, List<PerkDefinition> perks) {
        Set<String> requiredTags = new HashSet<>(definition.requiredTags());
        if (requiredTags.isEmpty()) {
            return (int) perks.stream()
                    .filter(perk -> definition.requiredPerks().contains(perk.id()))
                    .count();
        }

        return (int) perks.stream()
                .filter(perk -> perk.tags().stream().anyMatch(requiredTags::contains))
                .count();
    }

    /** Extreu els identificadors de les perks. */
    private Set<String> perkIds(List<PerkDefinition> perks) {
        Set<String> ids = new HashSet<>();
        if (perks != null) {
            for (PerkDefinition perk : perks) {
                if (perk != null)
                    ids.add(perk.id());
            }
        }
        return ids;
    }

    /** Estat intern d’una sinergia activa. */
    private record ActiveSynergy(SynergyDefinition definition, int members, int rank, SynergyLevel level) {
    }
}
