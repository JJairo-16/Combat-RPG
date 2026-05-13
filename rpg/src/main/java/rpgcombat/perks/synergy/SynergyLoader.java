package rpgcombat.perks.synergy;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.perks.synergy.SynergyConfig.LevelConfig;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Carrega definicions de sinergies des d’un fitxer JSON. */
public final class SynergyLoader {
    private static final Gson GSON = new Gson();

    private SynergyLoader() {
    }

    /** Llegeix i converteix el fitxer indicat en sinergies. */
    public static List<SynergyDefinition> load(Path path) throws IOException {
        if (path == null || !Files.exists(path))
            return List.of();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            SynergyConfig[] raw = GSON.fromJson(reader, SynergyConfig[].class);
            if (raw == null)
                return List.of();

            List<SynergyDefinition> definitions = new ArrayList<>(raw.length);
            for (SynergyConfig cfg : raw) {
                if (cfg == null || blank(cfg.id()))
                    continue;
                definitions.add(toDefinition(cfg));
            }
            validateSingleOverridePerPerk(definitions);
            return List.copyOf(definitions);
        }
    }

    /** Converteix una configuració crua en definició de sinergia. */
    private static SynergyDefinition toDefinition(SynergyConfig cfg) {
        SynergyType type = enumValue(SynergyType.class, value(cfg.type(), "BONUS_EXTRA"), cfg.id(), "type");
        return new SynergyDefinition(
                cfg.id(),
                value(cfg.name(), cfg.id()),
                value(cfg.description(), ""),
                type,
                cfg.requiredPerks(),
                cfg.requiredTags(),
                cfg.minMembers() == null ? 1 : Math.max(1, cfg.minMembers()),
                scaling(cfg.scaling(), cfg.id()),
                alterations(cfg.alterations(), cfg.id()));
    }

    /** Converteix la configuració d’escalat. */
    private static SynergyScaling scaling(SynergyConfig.ScalingConfig cfg, String id) {
        if (cfg == null)
            return new SynergyScaling(false, List.of());
        
        List<SynergyLevel> levels = getLevels(cfg, id);
        return new SynergyScaling(Boolean.TRUE.equals(cfg.enabled()), levels);
    }

    private static List<SynergyLevel> getLevels(SynergyConfig.ScalingConfig cfg, String id) {
        if (cfg.levels() == null)
            return List.of();

        List<LevelConfig> levels = cfg.levels();
        List<SynergyLevel> out = new ArrayList<>();

        for (LevelConfig level : levels) {
            if (level == null)
                continue;

            int members = level.members() == null ? 1 : Math.max(1, level.members());
            Phase phase = enumValue(Phase.class, value(level.trigger(), "AFTER_HIT"), id, "scaling.trigger");
            List<Rule> conditions = rules(level.conditions());
            List<Rule> actions = rules(level.actions());

            out.add(new SynergyLevel(members, phase, conditions, actions));
        }

        return out;
    }

    /** Converteix les alteracions de membres. */
    private static List<MemberAlteration> alterations(List<SynergyConfig.AlterationConfig> configs, String id) {
        if (configs == null || configs.isEmpty())
            return List.of();
        return configs.stream()
                .filter(alt -> alt != null && !blank(alt.memberPerkId()))
                .map(alt -> new MemberAlteration(
                        alt.memberPerkId(),
                        enumValue(Phase.class, value(alt.trigger(), "AFTER_HIT"), id, "alteration.trigger"),
                        rules(alt.conditions()),
                        rules(alt.actions()),
                        value(alt.descriptionAppend(), "")))
                .toList();
    }

    /** Converteix regles configurades en regles de perk. */
    private static List<Rule> rules(List<SynergyConfig.RuleConfig> configs) {
        if (configs == null || configs.isEmpty())
            return List.of();
        return configs.stream()
                .filter(rule -> rule != null && !blank(rule.type()))
                .map(rule -> new Rule(rule.type(), rule.params() == null ? Map.of() : rule.params()))
                .toList();
    }

    /** Valida que una perk només tingui un override ALTER_MEMBERS. */
    private static void validateSingleOverridePerPerk(List<SynergyDefinition> definitions) {
        Map<String, String> ownerByMemberPerk = new HashMap<>();
        for (SynergyDefinition synergy : definitions) {
            if (synergy.type() != SynergyType.ALTER_MEMBERS)
                continue;
            for (MemberAlteration alteration : synergy.alterations()) {
                String memberPerkId = alteration.memberPerkId();
                String previousSynergy = ownerByMemberPerk.putIfAbsent(memberPerkId, synergy.id());
                if (previousSynergy != null) {
                    throw new IllegalStateException(
                            "synergies.json invàlid: la perk '" + memberPerkId
                                    + "' és alterada per més d'una sinergia ('"
                                    + previousSynergy + "' i '" + synergy.id()
                                    + "'). Només es permet un override ALTER_MEMBERS per perk.");
                }
            }
        }
    }

    /** Converteix un text en valor d’enum o informa de l’error. */
    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String id, String fieldName) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor invàlid a " + id + " (" + fieldName + "): " + raw, ex);
        }
    }

    /** Retorna el valor indicat o el valor per defecte. */
    private static String value(String value, String def) {
        return blank(value) ? def : value;
    }

    /** Indica si el text és nul o buit. */
    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}