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
