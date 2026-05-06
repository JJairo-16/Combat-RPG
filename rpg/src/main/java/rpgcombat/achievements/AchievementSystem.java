package rpgcombat.achievements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    public void onSpiritualCallingUsed(Character player, int face, double healPercent, double healAmount, int roundNumber) {
        apply(AchievementUpdate.spiritualCallingUsed(player, face, healPercent, healAmount, roundNumber));
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
        if (!dirty) return;
        try {
            store.save(savePath, progressById.values());
            dirty = false;
        } catch (IOException e) {
            Prettier.warn("No s'ha pogut desar el progrés dels assoliments: " + e.getMessage());
        }
    }

    /** Aplica una actualització i persisteix immediatament qualsevol canvi. */
    private void apply(AchievementUpdate update) {
        boolean changed = false;
        for (AchievementProgress progress : progressById.values()) {
            changed |= progress.update(update);
        }

        if (changed) {
            dirty = true;
            saveIfDirty();
        }
    }
}
