package rpgcombat.settings;

/** Ajustos persistents de l'usuari. */
public record UserSettings(boolean showMomentumMessages) {
    /** Retorna els ajustos inicials quan encara no hi ha cap fitxer desat. */
    public static UserSettings defaults() {
        return new UserSettings(true);
    }
}
