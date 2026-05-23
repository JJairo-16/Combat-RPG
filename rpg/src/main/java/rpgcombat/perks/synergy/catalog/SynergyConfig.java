package rpgcombat.perks.synergy.catalog;

import java.util.List;
import java.util.Map;

/** Configuració d’una sinergia de perks. */
record SynergyConfig(
        String id,
        String name,
        String description,
        String type,
        List<String> requiredPerks,
        List<String> requiredTags,
        Integer minMembers,
        ScalingConfig scaling,
        List<AlterationConfig> alterations) {

    /** Configuració d’escalat per nombre de membres. */
    record ScalingConfig(Boolean enabled, List<LevelConfig> levels) {}

    /** Defineix un nivell d’escalat amb condicions i accions. */
    record LevelConfig(
            Integer members,
            String trigger,
            List<RuleConfig> conditions,
            List<RuleConfig> actions) {}

    /** Alteració aplicada a un perk concret dins la sinergia. */
    record AlterationConfig(
            String memberPerkId,
            String trigger,
            List<RuleConfig> conditions,
            List<RuleConfig> actions,
            String descriptionAppend) {}

    /** Representa una regla configurable (condició o acció). */
    record RuleConfig(String type, Map<String, Object> params) {}
}
