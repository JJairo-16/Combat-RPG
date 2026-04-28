package rpgcombat.perks.config;

import java.util.List;

/**
 * Representa la configuració d'una missió definida en JSON.
 *
 * @param id identificador únic de la missió
 * @param name nom de la missió
 * @param description descripció de la missió
 * @param weight pes o probabilitat d'aparició
 * @param objective configuració de l'objectiu de la missió
 */
public record MissionConfig(
        String id,
        String name,
        String description,
        Integer weight,
        ObjectiveConfig objective) {

    /**
     * Defineix els paràmetres de l'objectiu de la missió.
     *
     * @param type tipus d'objectiu
     * @param event esdeveniment que el fa avançar
     * @param successEvent esdeveniment que indica èxit
     * @param resetEvent esdeveniment que reinicia el progrés
     * @param sequence seqüència d'esdeveniments requerida
     * @param target valor objectiu a assolir
     * @param value increment aplicat per esdeveniment
     * @param turns nombre de torns disponibles
     */
    public record ObjectiveConfig(
            String type,
            String event,
            String successEvent,
            String resetEvent,
            List<String> sequence,
            Double target,
            Double value,
            Integer turns) {}
}