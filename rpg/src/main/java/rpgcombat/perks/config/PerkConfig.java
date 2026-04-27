package rpgcombat.perks.config;

import java.util.List;
import java.util.Map;

/** Configuració JSON d'una perk. */
public record PerkConfig(
        String id,
        String name,
        String description,
        String family,
        String trigger,
        Integer weight,
        List<RuleConfig> conditions,
        List<RuleConfig> actions) {

    public record RuleConfig(String type, Map<String, Object> params) {}
}
