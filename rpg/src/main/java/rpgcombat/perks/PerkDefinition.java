package rpgcombat.perks;

import java.util.List;
import java.util.Map;

import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Definició lleugera d'una perk carregada des de JSON.
 *
 * @param id identificador únic
 * @param name nom de la perk
 * @param description descripció
 * @param family família de la perk
 * @param trigger fase que l'activa
 * @param weight pes o probabilitat d'aparició
 * @param conditions condicions requerides
 * @param actions accions executades
 */
public record PerkDefinition(
        String id,
        String name,
        String description,
        PerkFamily family,
        Phase trigger,
        int weight,
        List<Rule> conditions,
        List<Rule> actions) {

    /**
     * Bloc configurable de condició o acció.
     *
     * @param type tipus de regla
     * @param params paràmetres de la regla
     */
    public record Rule(String type, Map<String, Object> params) {}

}