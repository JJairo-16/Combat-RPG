package rpgcombat.perks.divine;

import java.util.List;
import java.util.Map;

/**
 * Configuració JSON d'una perk divina inicial.
 */
public record DivinePerkConfig(
        String id,
        String name,
        String god,
        String shortDescription,
        String description,
        List<String> allowedBreeds,
        String trigger,
        Integer weight,
        List<RuleConfig> conditions,
        List<RuleConfig> actions,
        AwakeningConfig awakening,
        List<String> tags) {

    /**
     * Defineix una regla genèrica amb tipus i paràmetres.
     */
    public record RuleConfig(String type, Map<String, Object> params) {}

    /**
     * Defineix la configuració del sistema de despertar.
     */
    public record AwakeningConfig(
            Integer maxCharge,
            String chargeBy,
            String description) {}
}