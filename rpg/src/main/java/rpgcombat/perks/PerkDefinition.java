package rpgcombat.perks;

import java.util.List;
import java.util.Map;

import rpgcombat.weapons.passives.HitContext.Phase;

/** Definició lleugera d'una perk carregada des de JSON. */
public record PerkDefinition(
        String id,
        String name,
        String description,
        PerkFamily family,
        Phase trigger,
        int weight,
        List<Rule> conditions,
        List<Rule> actions) {

    /** Bloc configurable de condició o acció. */
    public record Rule(String type, Map<String, Object> params) {}

    public String menuLine() {
        return familyLabel() + " " + name + " — " + description;
    }

    private String familyLabel() {
        return switch (family) {
            case STRATEGY -> "[Estratègia]";
            case LUCK -> "[Sort]";
            case CHAOS -> "[Caos]";
            case CORRUPTED -> "[Corrupta]";
        };
    }
}
