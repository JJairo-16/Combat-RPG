package rpgcombat.achievements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementVisibility;
import rpgcombat.achievements.persistence.AchievementStore;
import rpgcombat.achievements.ui.Achievement;
import rpgcombat.combat.models.Winner;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.ui.Prettier;

/** Coordina assoliments globals, progrés i persistència. */
public final class AchievementSystem {
    private final AchievementStore store;
    private final Path savePath;
    private final Map<String, AchievementProgress> progressById;
    private boolean dirty;
    private final Map<String, MatchAchievementMemory> matchMemoryByActor = new HashMap<>();

    private int pendingCompletedCount = 0;

    private AchievementSystem(AchievementStore store, Path savePath, Map<String, AchievementProgress> progressById) {
        this.store = store;
        this.savePath = savePath;
        this.progressById = progressById;
    }

    /** Crea el sistema carregant progrés global des de l'AppData. */
    public static AchievementSystem load(Collection<AchievementDefinition> definitions, String configuredSavePath) {
        AchievementStore store = new AchievementStore();
        Path savePath = store.resolveAppDataPath(configuredSavePath);
        Map<String, AchievementProgress> progress = store.load(savePath, definitions);
        return new AchievementSystem(store, savePath, progress);
    }

    /** Actualitza assoliments a partir d'un torn i desa si hi ha canvis. */
    public void onTurn(AchievementUpdate update) {
        apply(update);
    }

    /** Registra el final del combat una sola vegada perquè el progrés és global. */
    public void onMatchFinished(Winner winner, Character player1, Character player2, int roundNumber) {
        apply(AchievementUpdate.fromMatchFinished(player1, player2, winner, roundNumber));
    }

    /** Registra que s'ha equipat una arma. */
    public void onWeaponEquipped(Character player) {
        apply(AchievementUpdate.simple(player, AchievementEvent.WEAPON_EQUIPPED));
    }

    /** Registra que una missió de perk ha avançat sense completar-se encara. */
    public void onPerkMissionProgress(Character player, String missionId, String perkId, double progressBefore,
            double progressAfter, double target, int activeMissionCount, int roundNumber) {
        apply(AchievementUpdate.perkMissionProgress(player, missionId, perkId, progressBefore, progressAfter,
                target, activeMissionCount, roundNumber));
    }

    /** Registra que una missió de perk s'ha completat. */
    public void onPerkMissionCompleted(Character player, String missionId, String perkId,
            int completedMissionCount, int activeMissionCount, int roundNumber) {
        apply(AchievementUpdate.perkMissionCompleted(player, missionId, perkId, completedMissionCount,
                activeMissionCount, roundNumber));
    }

    /** Registra que el jugador ha obtingut una perk. */
    public void onPerkGained(Character player, String perkId, String perkName, String family,
            java.util.List<String> tags, int perkCount, int maxPerks, int roundNumber) {
        apply(AchievementUpdate.perkGained(player, perkId, perkName, family, tags, perkCount, maxPerks, roundNumber));
    }

    /** Registra la perk divina inicial del jugador. */
    public void onDivinePerkAssigned(Character player, String divinePerkId, String divinePerkName,
            String god, int roundNumber) {
        apply(AchievementUpdate.divinePerkAssigned(player, divinePerkId, divinePerkName, god, roundNumber));
    }

    /** Registra que una sinergia s'ha activat per primera vegada en el combat. */
    public void onSynergyActivated(Character player, String synergyId, String synergyName,
            int synergyCount, int roundNumber) {
        apply(AchievementUpdate.synergyActivated(player, synergyId, synergyName, synergyCount, roundNumber));
    }

    /** Registra que s'ha afegit el trigger de Caos a un jugador. */
    public void onChaosTriggerAdded(Character player, int roundNumber) {
        apply(AchievementUpdate.chaosTriggerAdded(player, roundNumber));
    }

    /** Registra que el combat ha començat amb el mode caòtic actiu. */
    public void onChaosMatchStarted(Character player1, Character player2, int roundNumber) {
        apply(AchievementUpdate.chaosMatchStarted(player1, player2, roundNumber));
    }

    /** Registra l'ús del Pacte de Sang. */
    public void onBloodPactUsed(Character player, double manaRestored, double hpCost, double hpCostPercent,
            double hpBeforePercent, double hpAfterPercent, double manaBeforePercent, double manaAfterPercent,
            int roundNumber) {
        apply(AchievementUpdate.bloodPactUsed(player, manaRestored, hpCost, hpCostPercent,
                hpBeforePercent, hpAfterPercent, manaBeforePercent, manaAfterPercent, roundNumber));
    }

    /** Registra l'ús de la Crida Espiritual. */
    public void onSpiritualCallingUsed(Character player, int face, double healPercent, double healAmount,
            int roundNumber) {
        apply(AchievementUpdate.spiritualCallingUsed(player, face, healPercent, healAmount, roundNumber));
    }

    /** Registra l'ús d'una ulti de segona etapa. */
    public void onUltimateUsed(Character player, String ultimateId, String ultimateName, String ultimateWeaponType,
            int roundNumber) {
        apply(AchievementUpdate.ultimateUsed(player, ultimateId, ultimateName, ultimateWeaponType, roundNumber));
    }

    /** Converteix el progrés intern a models visuals. */
    public List<Achievement> toViewModels() {
        List<Achievement> result = new ArrayList<>();
        for (AchievementProgress progress : progressById.values()) {
            AchievementDefinition definition = progress.definition();
            AchievementVisibility visibility = definition.visibility() == null
                    ? AchievementVisibility.visible()
                    : definition.visibility();
            result.add(new Achievement(
                    definition.name(),
                    definition.description(),
                    progress.viewProgress(),
                    progress.viewGoal(),
                    visibility.showNameBeforeComplete(),
                    visibility.showDescriptionBeforeComplete()));
        }
        return result;
    }

    /** Desa el progrés si hi ha canvis pendents. */
    public void saveIfDirty() {
        if (!dirty)
            return;
        try {
            store.save(savePath, progressById.values());
            dirty = false;
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut desar el progrés dels assoliments: " + e.getMessage());
        }
    }

    public int consumePendingCompletedCount() {
        int count = pendingCompletedCount;
        pendingCompletedCount = 0;
        return count;
    }

    /** Consulta si un assoliment global ja està completat. */
    public boolean isCompleted(String achievementId) {
        if (achievementId == null || achievementId.isBlank()) {
            return false;
        }
        AchievementProgress progress = progressById.get(achievementId);
        return progress != null && progress.completed();
    }

    /** Nombre total d'assoliments completats. */
    public int completedCount() {
        int count = 0;
        for (AchievementProgress progress : progressById.values()) {
            if (progress.completed()) {
                count++;
            }
        }
        return count;
    }

    /** Aplica una actualització i persisteix immediatament qualsevol canvi. */
    private void apply(AchievementUpdate update) {
        Objects.requireNonNull(update, "La informació de l'event no pot ser nula.");
        
        AchievementUpdate effectiveUpdate = enrichWithMatchMemory(update);
        boolean changed = false;
        int completedNow = 0;

        for (AchievementProgress progress : progressById.values()) {
            boolean wasCompleted = progress.completed();

            boolean progressChanged = progress.update(effectiveUpdate);
            changed |= progressChanged;

            if (!wasCompleted && progress.completed()) {
                completedNow++;
            }
        }

        rememberMatchFacts(effectiveUpdate);

        if (effectiveUpdate.has(AchievementEvent.MATCH_FINISHED)) {
            matchMemoryByActor.clear();
        }

        if (completedNow > 0) {
            pendingCompletedCount += completedNow;
        }

        if (changed) {
            dirty = true;
            saveIfDirty();
        }
    }

    /** Afegeix esdeveniments derivats que necessiten memòria del combat actual. */
    private AchievementUpdate enrichWithMatchMemory(AchievementUpdate update) {
        if (update == null) return null;

        EnumSet<AchievementEvent> extraEvents = EnumSet.noneOf(AchievementEvent.class);
        Map<String, Object> extraFields = new HashMap<>();
        String actorKey = actorKey(update);
        MatchAchievementMemory actorMemory = actorKey == null ? new MatchAchievementMemory()
                : matchMemoryByActor.getOrDefault(actorKey, new MatchAchievementMemory());

        if (update.has(AchievementEvent.BLOOD_PACT_USED) && actorMemory.spiritualCallingUsed) {
            extraEvents.add(AchievementEvent.BLOOD_PACT_AND_SPIRITUAL_CALLING_SAME_MATCH);
            extraFields.put("bloodPactAndSpiritualCallingSameMatch", true);
        }
        if (update.has(AchievementEvent.SPIRITUAL_CALLING_USED) && actorMemory.bloodPactUsed) {
            extraEvents.add(AchievementEvent.BLOOD_PACT_AND_SPIRITUAL_CALLING_SAME_MATCH);
            extraFields.put("bloodPactAndSpiritualCallingSameMatch", true);
        }
        if (update.has(AchievementEvent.LIFE_STEAL) && actorMemory.bloodPactUsed) {
            extraEvents.add(AchievementEvent.LIFE_STEAL_AFTER_BLOOD_PACT);
            extraFields.put("lifeStealAfterBloodPact", true);
        }
        if (update.has(AchievementEvent.GRIMOIRE_CODE_SOLVED) && actorMemory.bloodPactLifePaid) {
            extraEvents.add(AchievementEvent.GRIMOIRE_CODE_SOLVED_AFTER_BLOOD_PACT_LIFE_PAID);
            extraFields.put("grimoireCodeSolvedAfterBloodPactLifePaid", true);
        }

        if (update.has(AchievementEvent.MATCH_FINISHED)) {
            if (update.has(AchievementEvent.MATCH_WON)) {
                if (!actorMemory.criticalHitDone) {
                    extraEvents.add(AchievementEvent.MATCH_WON_WITHOUT_CRIT);
                    extraFields.put("winnerHadCrit", false);
                }
                if (!actorMemory.activablePerkUsed) {
                    extraEvents.add(AchievementEvent.MATCH_WON_WITHOUT_PERK_ACTIVATION);
                    extraFields.put("winnerUsedActivablePerk", false);
                }
            }

            boolean incompleteMission = matchMemoryByActor.values().stream()
                    .anyMatch(memory -> memory.perkMissionProgressed && !memory.perkMissionCompleted);
            if (incompleteMission) {
                extraEvents.add(AchievementEvent.MATCH_FINISHED_WITH_INCOMPLETE_PERK_MISSION);
                extraFields.put("matchHadIncompletePerkMission", true);
            }
        }

        return update.withAdditional(Set.copyOf(extraEvents), extraFields);
    }

    /** Desa fets puntuals que els assoliments necessiten consultar més tard dins del combat. */
    private void rememberMatchFacts(AchievementUpdate update) {
        if (update == null || update.has(AchievementEvent.MATCH_FINISHED)) return;
        String actorKey = actorKey(update);
        if (actorKey == null || actorKey.isBlank()) return;

        MatchAchievementMemory memory = matchMemoryByActor.computeIfAbsent(actorKey, key -> new MatchAchievementMemory());
        if (update.has(AchievementEvent.CRIT)) memory.criticalHitDone = true;
        if (update.has(AchievementEvent.PERK_ACTIVATED)) memory.activablePerkUsed = true;
        if (update.has(AchievementEvent.PERK_MISSION_PROGRESS)) memory.perkMissionProgressed = true;
        if (update.has(AchievementEvent.PERK_MISSION_COMPLETED)) memory.perkMissionCompleted = true;
        if (update.has(AchievementEvent.BLOOD_PACT_USED)) memory.bloodPactUsed = true;
        if (update.has(AchievementEvent.BLOOD_PACT_LIFE_PAID)) memory.bloodPactLifePaid = true;
        if (update.has(AchievementEvent.SPIRITUAL_CALLING_USED)) memory.spiritualCallingUsed = true;
    }

    /** Clau estable de l'actor que origina l'actualització. */
    private String actorKey(AchievementUpdate update) {
        if (update == null) return null;
        String key = update.actorKey();
        if (key != null && !key.isBlank()) return key;
        Character owner = update.owner();
        return owner == null ? null : owner.getName();
    }

    /** Memòria mínima i no persistent del combat actual. */
    private static final class MatchAchievementMemory {
        boolean criticalHitDone;
        boolean activablePerkUsed;
        boolean perkMissionProgressed;
        boolean perkMissionCompleted;
        boolean bloodPactUsed;
        boolean bloodPactLifePaid;
        boolean spiritualCallingUsed;
    }

}
