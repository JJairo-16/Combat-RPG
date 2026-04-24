package rpgcombat.utils.cinematic.typing;

import rpgcombat.utils.cinematic.style.CinematicColor;

/**
 * Manté l'estat d'estil durant l'anàlisi del text.
 */
class TypingStyle {
    private CinematicColor color;
    private double speedMultiplier;
    private double pauseMultiplier;
    private double variationMultiplier;

    /**
     * Crea un estil inicial a partir del color i el to.
     */
    TypingStyle(CinematicColor color, TypingMood mood) {
        this.color = color;
        this.speedMultiplier = 1.0;
        this.pauseMultiplier = 1.0;
        this.variationMultiplier = 1.0;
        applyMood(mood);
    }

    /**
     * Constructor intern per copiar l'estil.
     */
    private TypingStyle(CinematicColor color, double speedMultiplier, double pauseMultiplier, double variationMultiplier) {
        this.color = color;
        this.speedMultiplier = speedMultiplier;
        this.pauseMultiplier = pauseMultiplier;
        this.variationMultiplier = variationMultiplier;
    }

    /**
     * Retorna una còpia de l'estil actual.
     */
    TypingStyle copy() {
        return new TypingStyle(color, speedMultiplier, pauseMultiplier, variationMultiplier);
    }

    /**
     * Retorna el color actual.
     */
    CinematicColor color() {
        return color;
    }

    /**
     * Defineix el color.
     */
    void color(CinematicColor color) {
        this.color = color;
    }

    /**
     * Aplica un to a l'estil.
     */
    void applyMood(TypingMood mood) {
        if (mood == null) return;
        this.speedMultiplier *= mood.speedMultiplier();
        this.pauseMultiplier *= mood.pauseMultiplier();
        this.variationMultiplier *= mood.variationMultiplier();
    }

    /**
     * Aplica un multiplicador de velocitat.
     */
    void speedMultiplier(double value) {
        this.speedMultiplier *= value;
    }

    /**
     * Aplica un multiplicador de pauses.
     */
    void pauseMultiplier(double value) {
        this.pauseMultiplier *= value;
    }

    /**
     * Aplica un multiplicador de variació.
     */
    void variationMultiplier(double value) {
        this.variationMultiplier *= value;
    }

    /**
     * Retorna el multiplicador de velocitat.
     */
    double speedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Retorna el multiplicador de pauses.
     */
    double pauseMultiplier() {
        return pauseMultiplier;
    }

    /**
     * Retorna el multiplicador de variació.
     */
    double variationMultiplier() {
        return variationMultiplier;
    }
}