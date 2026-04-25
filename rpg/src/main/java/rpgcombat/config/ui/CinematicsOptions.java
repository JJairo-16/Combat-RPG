package rpgcombat.config.ui;

/**
 * Opcions d'activació de les cinemàtiques del joc.
 */
public record CinematicsOptions(
    boolean preCreation,
    boolean postCreation,
    boolean antiStall,
    double chaos
) {
    /** Probabilitat per defecte d'activar Caos. */
    public static final double CHAOS_RATE = 0.15;

    /**
     * Configuració estàndard amb cinemàtiques activades.
     */
    public static CinematicsOptions defaultConfig() {
        return new CinematicsOptions(true, true, true, CHAOS_RATE);
    }

    /**
     * Configuració de depuració sense cinemàtiques.
     */
    public static CinematicsOptions debugConfig() {
        return new CinematicsOptions(false, false, false, CHAOS_RATE);
    }
}