package rpgcombat.perks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rpgcombat.perks.mission.MissionProgress;

/**
 * Manté l'estat de missions, perks i sinergies d'un jugador en combat.
 */
public final class PlayerPerkState {
    private final List<MissionProgress> missions = new ArrayList<>();
    private final List<PerkDefinition> perks = new ArrayList<>();
    private boolean pendingChoice;
    private Map<String, List<String>> synergyDescriptions = Map.of();
    private List<String> activeSynergyNames = List.of();

    public static final int MAX_PERKS = 4;

    /** Crea l'estat del jugador amb una missió assignada. */
    public PlayerPerkState(MissionProgress mission) {
        addMission(mission);
    }

    /** Crea un estat buit. */
    public PlayerPerkState() {
    }

    public boolean canGainMorePerks() {
        return perks.size() < MAX_PERKS;
    }

    public boolean hasPerk(String perkId) {
        return perkId != null && perks.stream().anyMatch(p -> p.id().equals(perkId));
    }

    public int perkCount() {
        return perks.size();
    }

    /** Afegeix una missió activa si és vàlida. */
    public void addMission(MissionProgress mission) {
        if (mission != null)
            missions.add(mission);
    }

    /** @return vista immutable de les missions del jugador. */
    public List<MissionProgress> missions() {
        return Collections.unmodifiableList(missions);
    }

    /**
     * Compatibilitat amb el flux actual: retorna la primera missió pendent de
     * recompensa, o si no n'hi ha, la primera missió existent.
     */
    public MissionProgress mission() {
        return missions.stream()
                .filter(m -> !m.rewardClaimed())
                .findFirst()
                .orElse(missions.isEmpty() ? null : missions.get(0));
    }

    /** @return vista immutable de les perks triades pel jugador. */
    public List<PerkDefinition> perks() {
        return Collections.unmodifiableList(perks);
    }

    /** @return última perk triada, mantenint compatibilitat amb codi antic. */
    public PerkDefinition chosenPerk() {
        return perks.isEmpty() ? null : perks.get(perks.size() - 1);
    }

    /** Desa una perk triada com a recompensa. */
    public void addPerk(PerkDefinition chosenPerk) {
        if (chosenPerk != null && perks.stream().noneMatch(p -> p.id().equals(chosenPerk.id()))) {
            perks.add(chosenPerk);
        }
    }

    /** Compatibilitat amb el nom anterior. */
    public void setChosenPerk(PerkDefinition chosenPerk) {
        addPerk(chosenPerk);
    }

    /** @return si hi ha una elecció de perk pendent. */
    public boolean pendingChoice() {
        return pendingChoice;
    }

    /** Marca que hi ha una elecció pendent si alguna missió està completada. */
    public void updatePendingChoice() {
        pendingChoice = missions.stream().anyMatch(m -> m.completed() && !m.rewardClaimed());
    }

    /** Marca com reclamada la primera missió completada amb recompensa pendent. */
    public void clearPendingChoice() {
        pendingChoice = false;
        missions.stream()
                .filter(m -> m.completed() && !m.rewardClaimed())
                .findFirst()
                .ifPresent(MissionProgress::markRewardClaimed);
        updatePendingChoice();
    }

    public List<String> synergyDescriptionsFor(String perkId) {
        if (perkId == null) return List.of();
        return synergyDescriptions.getOrDefault(perkId, List.of());
    }

    public void setSynergyDescriptions(Map<String, List<String>> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            synergyDescriptions = Map.of();
            return;
        }

        Map<String, List<String>> copy = new HashMap<>();
        descriptions.forEach((key, value) -> {
            if (key != null && value != null && !value.isEmpty()) {
                copy.put(key, List.copyOf(value));
            }
        });
        synergyDescriptions = Map.copyOf(copy);
    }

    public List<String> activeSynergyNames() {
        return activeSynergyNames;
    }

    public void setActiveSynergyNames(List<String> names) {
        activeSynergyNames = names == null ? List.of() : names.stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    public List<String> missionIds() {
        return missions.stream()
                .filter(m -> m.definition() != null)
                .map(m -> m.definition().id())
                .toList();
    }
}
