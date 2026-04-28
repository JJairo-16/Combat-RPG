package rpgcombat.perks.config;

import java.util.List;
import java.util.Map;

/**
 * Representa la configuració d'una perk definida en JSON.
 *
 * @param id identificador únic de la perk
 * @param name nom de la perk
 * @param description descripció de la perk
 * @param family família o categoria
 * @param trigger esdeveniment que la dispara
 * @param weight pes o probabilitat d'aparició
 * @param conditions llista de condicions a complir
 * @param actions llista d'accions a executar
 */
public record PerkConfig(
        String id,
        String name,
        String description,
        String family,
        String trigger,
        Integer weight,
        List<RuleConfig> conditions,
        List<RuleConfig> actions) {

    /**
     * Defineix una regla amb tipus i paràmetres.
     *
     * @param type tipus de regla
     * @param params paràmetres associats
     */
    public record RuleConfig(String type, Map<String, Object> params) {}
}