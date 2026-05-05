package rpgcombat.perks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.perks.mission.MissionProgress;

/**
 * Estat de perks, missions i sinergies d'un jugador durant el combat.
 */
public final class PlayerPerkState {
    private final List<MissionProgress> missions = new ArrayList<>();
    private final List<PerkDefinition> perks = new ArrayList<>();
    private boolean pendingChoice;
    private Map<String, List<String>> synergyDescriptions = Map.of();
    private List<String> activeSynergyNames = List.of();
    private DivinePerkDefinition divinePerk;

    /** Nombre màxim de perks equipables. */
    public static final int MAX_PERKS = 4;

    /** Inicialitza amb una missió activa. */
    public PlayerPerkState(MissionProgress mission) {
        addMission(mission);
    }

    /** Inicialitza buit. */
    public PlayerPerkState() {
    }

    /** Indica si el jugador pot obtenir més perks. */
    public boolean canGainMorePerks() {
        return perks.size() < MAX_PERKS;
    }

    /** Comprova si el jugador ja té una perk concreta. */
    public boolean hasPerk(String perkId) {
        return perkId != null && perks.stream().anyMatch(p -> p.id().equals(perkId));
    }

    /** Nombre de perks actuals. */
    public int perkCount() {
        return perks.size();
    }

    /** Afegeix una missió si és vàlida. */
    public void addMission(MissionProgress mission) {
        if (mission != null)
            missions.add(mission);
    }

    /** Retorna les missions (només lectura). */
    public List<MissionProgress> missions() {
        return Collections.unmodifiableList(missions);
    }

    /**
     * Retorna una missió rellevant:
     * primer pendent de recompensa, sinó la primera.
     */
    public MissionProgress mission() {
        return missions.stream()
                .filter(m -> !m.rewardClaimed())
                .findFirst()
                .orElse(missions.isEmpty() ? null : missions.get(0));
    }

    /** Retorna la perk divina activa. */
    public DivinePerkDefinition divinePerk() {
        return divinePerk;
    }

    /** Assigna la perk divina. */
    public void setDivinePerk(DivinePerkDefinition divinePerk) {
        this.divinePerk = divinePerk;
    }

    /** Retorna les perks del jugador (només lectura). */
    public List<PerkDefinition> perks() {
        return Collections.unmodifiableList(perks);
    }

    /** Retorna l'última perk triada. */
    public PerkDefinition chosenPerk() {
        return perks.isEmpty() ? null : perks.get(perks.size() - 1);
    }

    /** Afegeix una perk si no està repetida. */
    public void addPerk(PerkDefinition chosenPerk) {
        if (chosenPerk != null && perks.stream().noneMatch(p -> p.id().equals(chosenPerk.id()))) {
            perks.add(chosenPerk);
        }
    }

    /** Alias per compatibilitat amb codi antic. */
    public void setChosenPerk(PerkDefinition chosenPerk) {
        addPerk(chosenPerk);
    }

    /** Indica si hi ha una elecció de perk pendent. */
    public boolean pendingChoice() {
        return pendingChoice;
    }

    /** Marca si alguna missió completada requereix elecció. */
    public void updatePendingChoice() {
        pendingChoice = missions.stream().anyMatch(m -> m.completed() && !m.rewardClaimed());
    }

    /** Consumeix la recompensa pendent de la primera missió completada. */
    public void clearPendingChoice() {
        pendingChoice = false;
        missions.stream()
                .filter(m -> m.completed() && !m.rewardClaimed())
                .findFirst()
                .ifPresent(MissionProgress::markRewardClaimed);
        updatePendingChoice();
    }

    /** Retorna descripcions de sinergia per perk. */
    public List<String> synergyDescriptionsFor(String perkId) {
        if (perkId == null) return List.of();
        return synergyDescriptions.getOrDefault(perkId, List.of());
    }

    /** Defineix les descripcions de sinergia actives. */
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

    /** Noms de sinergies actives. */
    public List<String> activeSynergyNames() {
        return activeSynergyNames;
    }

    /** Defineix els noms de sinergies actives. */
    public void setActiveSynergyNames(List<String> names) {
        activeSynergyNames = names == null ? List.of() : names.stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    /** Retorna els identificadors de missions actuals. */
    public List<String> missionIds() {
        return missions.stream()
                .filter(m -> m.definition() != null)
                .map(m -> m.definition().id())
                .toList();
    }
}
