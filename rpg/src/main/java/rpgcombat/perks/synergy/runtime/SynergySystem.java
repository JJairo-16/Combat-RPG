package rpgcombat.perks.synergy.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PlayerPerkState;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.perks.effect.SynergyBonusEffect;
import rpgcombat.perks.synergy.model.MemberAlteration;
import rpgcombat.perks.synergy.model.SynergyDefinition;
import rpgcombat.perks.synergy.model.SynergyLevel;
import rpgcombat.perks.synergy.model.SynergyType;
import rpgcombat.perks.synergy.view.SynergyDisplayInfo;
import rpgcombat.perks.synergy.view.SynergyPreview;

/** Calcula, previsualitza i aplica sinergies de perks. */
public final class SynergySystem {
    private final List<SynergyDefinition> definitions;
    private final Map<PlayerPerkState, ActiveSynergyCache> activeCache = new IdentityHashMap<>();

    private static final Comparator<ActiveSynergy> SINERGY_ORDER = Comparator.comparing(a -> a.definition().id());

    /** Inicialitza el sistema amb les sinergies disponibles. */
    public SynergySystem(List<SynergyDefinition> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    /** Previsualitza les sinergies que activaria una perk candidata. */
    public SynergyPreview preview(PlayerPerkState state, PerkDefinition candidate) {
        if (state == null || candidate == null)
            return SynergyPreview.empty();

        return preview(PerkSnapshot.from(state.perks()), candidate);
    }

    /** Previsualitza una candidata reutilitzant l'estat indexat previ. */
    private SynergyPreview preview(PerkSnapshot before, PerkDefinition candidate) {
        PerkSnapshot after = before.with(candidate);

        Map<String, ActiveSynergy> beforeMap = activeMap(activeList(before));
        Map<String, ActiveSynergy> afterMap = activeMap(activeList(after));

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
        PerkSnapshot before = state == null ? PerkSnapshot.empty() : PerkSnapshot.from(state.perks());
        Map<String, SynergyPreview> result = new HashMap<>();
        for (PerkDefinition candidate : candidates) {
            if (candidate != null) {
                result.put(candidate.id(), state == null ? SynergyPreview.empty() : preview(before, candidate));
            }
        }
        return result;
    }

    /** Recalcula i aplica els efectes de sinergia al jugador. */
    public void refresh(Character player, PlayerPerkState state) {
        if (player == null || state == null)
            return;

        removeSynergyBonuses(player);

        PerkSnapshot perks = PerkSnapshot.from(state.perks());
        List<ActiveSynergy> active = activeList(perks);
        cacheActive(state, active);
        Map<String, List<MemberAlteration>> alterationsByPerk =
                activeAlterationsByPerk(active, perks.perkIds());
        Map<String, List<String>> descriptionsByPerk =
                activeAlterationDescriptions(active, perks.perkIds());

        for (PerkDefinition perk : perks.perks()) {
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

        return activeFor(state).displayInfo();
    }

    /** Retorna la vista activa cachejada mentre l'estat de perks no canviï. */
    private ActiveSynergyCache activeFor(PlayerPerkState state) {
        ActiveSynergyCache cached = activeCache.get(state);
        if (cached != null && cached.revision() == state.perkRevision()) {
            return cached;
        }

        return cacheActive(state, activeList(PerkSnapshot.from(state.perks())));
    }

    /** Desa la vista activa després d'un càlcul inevitable de sinergies. */
    private ActiveSynergyCache cacheActive(PlayerPerkState state, List<ActiveSynergy> active) {
        ActiveSynergyCache cached = new ActiveSynergyCache(
                state.perkRevision(),
                List.copyOf(active),
                displayInfo(active));
        activeCache.put(state, cached);
        return cached;
    }

    /** Converteix sinergies actives a models visuals una sola vegada per revisió. */
    private List<SynergyDisplayInfo> displayInfo(List<ActiveSynergy> activeSynergies) {
        return activeSynergies.stream()
                .map(synergy -> new SynergyDisplayInfo(
                        synergy.definition().id(),
                        synergy.definition().name(),
                        synergy.definition().description(),
                        synergy.definition().type(),
                        synergy.members(),
                        synergy.rank()))
                .toList();
    }

    /** Elimina els efectes extra de sinergia del jugador. */
    private void removeSynergyBonuses(Character player) {
        for (SynergyBonusEffect effect : player.effectsOfType(SynergyBonusEffect.class)) {
            player.removeEffect(effect.key());
        }
    }

    /** Agrupa les alteracions actives per perk afectada. */
    private Map<String, List<MemberAlteration>> activeAlterationsByPerk(
            List<ActiveSynergy> activeSynergies,
            Set<String> perkIds) {
        Map<String, List<MemberAlteration>> result = new HashMap<>();

        for (ActiveSynergy active : activeSynergies) {
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
    private Map<String, List<String>> activeAlterationDescriptions(
            List<ActiveSynergy> activeSynergies,
            Set<String> perkIds) {
        Map<String, List<String>> result = new HashMap<>();

        for (ActiveSynergy active : activeSynergies) {
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
    private Map<String, ActiveSynergy> activeMap(List<ActiveSynergy> activeSynergies) {
        Map<String, ActiveSynergy> result = new HashMap<>();
        for (ActiveSynergy active : activeSynergies) {
            result.put(active.definition().id(), active);
        }
        return result;
    }

    /** Calcula la llista de sinergies actives. */
    private List<ActiveSynergy> activeList(PerkSnapshot perks) {
        if (definitions.isEmpty() || perks == null || perks.perks().isEmpty())
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

        result.sort(SINERGY_ORDER);
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
    private boolean requiredPerksMet(SynergyDefinition definition, PerkSnapshot perks) {
        return perks.perkIds().containsAll(definition.requiredPerks());
    }

    /** Compta els membres que compleixen els requisits de la sinergia. */
    private int memberCount(SynergyDefinition definition, PerkSnapshot perks) {
        if (definition.requiredTags().isEmpty()) {
            return perks.countPresent(definition.requiredPerks());
        }

        return perks.countWithAnyTag(definition.requiredTags());
    }

    /** Snapshot indexat de les perks d'un jugador per ids i tags. */
    private record PerkSnapshot(
            List<PerkDefinition> perks,
            Set<String> perkIds,
            Map<String, Set<String>> perkIdsByTag) {

        static PerkSnapshot empty() {
            return new PerkSnapshot(List.of(), Set.of(), Map.of());
        }

        static PerkSnapshot from(List<PerkDefinition> source) {
            if (source == null || source.isEmpty()) {
                return empty();
            }

            List<PerkDefinition> perks = new ArrayList<>();
            Set<String> ids = new LinkedHashSet<>();
            Map<String, Set<String>> idsByTag = new HashMap<>();
            for (PerkDefinition perk : source) {
                if (perk == null || perk.id() == null || !ids.add(perk.id())) {
                    continue;
                }

                perks.add(perk);
                for (String tag : perk.tags()) {
                    idsByTag.computeIfAbsent(tag, ignored -> new LinkedHashSet<>()).add(perk.id());
                }
            }

            Map<String, Set<String>> immutableIdsByTag = new HashMap<>();
            idsByTag.forEach((tag, taggedIds) -> immutableIdsByTag.put(tag, Set.copyOf(taggedIds)));
            return new PerkSnapshot(List.copyOf(perks), Set.copyOf(ids), Map.copyOf(immutableIdsByTag));
        }

        PerkSnapshot with(PerkDefinition candidate) {
            if (candidate == null || candidate.id() == null || perkIds.contains(candidate.id())) {
                return this;
            }
            List<PerkDefinition> next = new ArrayList<>(perks);
            next.add(candidate);
            return from(next);
        }

        int countPresent(Collection<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (String id : ids) {
                if (perkIds.contains(id)) {
                    count++;
                }
            }
            return count;
        }

        int countWithAnyTag(Collection<String> tags) {
            if (tags == null || tags.isEmpty()) {
                return 0;
            }
            Set<String> matches = new LinkedHashSet<>();
            for (String tag : tags) {
                matches.addAll(perkIdsByTag.getOrDefault(tag, Set.of()));
            }
            return matches.size();
        }
    }

    /** Estat intern d’una sinergia activa. */
    private record ActiveSynergy(SynergyDefinition definition, int members, int rank, SynergyLevel level) {
    }

    /** Resultat actiu cachejat per revisió de l'estat de perks. */
    private record ActiveSynergyCache(
            long revision,
            List<ActiveSynergy> active,
            List<SynergyDisplayInfo> displayInfo) {
    }
}
