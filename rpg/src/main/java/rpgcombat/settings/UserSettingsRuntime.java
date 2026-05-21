package rpgcombat.settings;

import java.util.concurrent.atomic.AtomicReference;

import rpgcombat.terrain.model.TerrainSelectionMode;

/** Manté els ajustos actius en memòria durant l'execució. */
public final class UserSettingsRuntime {
    private static final AtomicReference<UserSettings> CURRENT = new AtomicReference<>(UserSettings.defaults());

    private UserSettingsRuntime() {
    }

    /** Aplica els ajustos carregats o desats. */
    public static void configure(UserSettings settings) {
        CURRENT.set(settings == null ? UserSettings.defaults() : settings);
    }

    /** Indica si cal mostrar missatges relacionats amb l'impuls. */
    public static boolean showMomentumMessages() {
        return CURRENT.get().showMomentumMessages();
    }

    /** Indica com s'ha de triar el terreny d'una partida. */
    public static TerrainSelectionMode terrainSelectionMode() {
        return CURRENT.get().terrainSelectionMode();
    }

    /** Retorna una còpia immutable dels ajustos actius. */
    public static UserSettings current() {
        return CURRENT.get();
    }
}
