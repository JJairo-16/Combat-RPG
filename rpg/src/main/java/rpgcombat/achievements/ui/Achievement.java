package rpgcombat.achievements.ui;

/**
 * Representa un assoliment del joc.
 *
 * @param name nom intern o visible de l'assoliment
 * @param description descripció de l'assoliment
 * @param progress progrés actual
 * @param goal objectiu necessari per completar-lo
 * @param showNameBeforeComplete indica si el nom es mostra abans de completar-lo
 * @param showDescriptionBeforeComplete indica si la descripció i el progrés es mostren abans de completar-lo
 */
public record Achievement(
        String name,
        String description,
        int progress,
        int goal,
        boolean showNameBeforeComplete,
        boolean showDescriptionBeforeComplete
) {
    /**
     * Indica si l'assoliment ja està completat.
     *
     * @return {@code true} si el progrés és igual o superior a l'objectiu
     */
    public boolean completed() {
        return goal > 0 && progress >= goal;
    }

    /**
     * Indica si l'assoliment està completament ocult.
     *
     * @return {@code true} si no es mostra ni el nom ni la descripció abans de completar-lo
     */
    public boolean hidden() {
        return !completed() && !showNameBeforeComplete && !showDescriptionBeforeComplete;
    }
}