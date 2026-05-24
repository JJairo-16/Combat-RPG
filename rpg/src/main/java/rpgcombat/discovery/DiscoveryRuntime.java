package rpgcombat.discovery;

import java.util.function.Supplier;

/** Punt global lleuger per registrar descobriments des de classes sense dependències d'aplicació. */
public final class DiscoveryRuntime {
    private static DiscoverySystem system;
    private static int suppressionDepth;

    private DiscoveryRuntime() {}

    /** Connecta el sistema actiu de descobriments. */
    public static void configure(DiscoverySystem activeSystem) {
        system = activeSystem;
    }

    /** Registra un descobriment si el sistema està disponible i el registre no està silenciat. */
    public static void discover(DiscoveryCategory category, String id) {
        if (suppressionDepth > 0) {
            return;
        }
        DiscoverySystem active = system;
        if (active != null) {
            active.discover(category, id);
        }
    }

    /** Desa els descobriments acumulats quan s'arriba a un checkpoint de partida. */
    public static void saveIfDirty() {
        DiscoverySystem active = system;
        if (active != null) {
            active.saveIfDirty();
        }
    }

    /** Desa els descobriments acumulats en el checkpoint d'inici de ronda. */
    public static void onRoundStart() {
        DiscoverySystem active = system;
        if (active != null) {
            active.onRoundStart();
        }
    }

    /**
     * Executa una operació sense registrar descobriments.
     *
     * <p>És útil per crear personatges, aplicar efectes interns inicials o preparar
     * dades auxiliars que encara no representen una descoberta real del jugador.</p>
     */
    public static void runSilently(Runnable action) {
        if (action == null) {
            return;
        }
        suppressionDepth++;
        try {
            action.run();
        } finally {
            suppressionDepth = Math.max(0, suppressionDepth - 1);
        }
    }

    /** Executa una operació amb retorn sense registrar descobriments. */
    public static <T> T callSilently(Supplier<T> action) {
        if (action == null) {
            return null;
        }
        suppressionDepth++;
        try {
            return action.get();
        } finally {
            suppressionDepth = Math.max(0, suppressionDepth - 1);
        }
    }
}
