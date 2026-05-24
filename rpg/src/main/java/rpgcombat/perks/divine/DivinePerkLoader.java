package rpgcombat.perks.divine;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import rpgcombat.models.breeds.Breed;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.perks.PerkFamily;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Carrega les perks divines inicials des de JSON.
 */
public final class DivinePerkLoader {
    private static final Gson GSON = new Gson();

    private DivinePerkLoader() {}

    /**
     * Llegeix el fitxer i construeix les definicions.
     */
    public static List<DivinePerkDefinition> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            DivinePerkConfig[] raw = GSON.fromJson(reader, DivinePerkConfig[].class);
            if (raw == null) return List.of();

            List<DivinePerkDefinition> result = new ArrayList<>(raw.length);
            for (DivinePerkConfig cfg : raw) {
                if (cfg == null || blank(cfg.id())) continue;
                result.add(toDefinition(cfg));
            }
            return List.copyOf(result);
        }
    }

    /**
     * Converteix una configuració en definició executable.
     */
    private static DivinePerkDefinition toDefinition(DivinePerkConfig cfg) {
        PerkDefinition perk = new PerkDefinition(
                cfg.id(),
                value(cfg.name(), cfg.id()),
                value(cfg.description(), ""),
                PerkFamily.DIVINE,
                enumValue(Phase.class, value(cfg.trigger(), "AFTER_HIT"), cfg.id(), "trigger"),
                cfg.weight() == null ? 1 : Math.max(1, cfg.weight()),
                rules(cfg.conditions()),
                rules(cfg.actions()),
                tags(cfg.tags()));

        DivinePerkConfig.AwakeningConfig awakening = cfg.awakening();
        int maxCharge = awakening == null || awakening.maxCharge() == null ? 0 : awakening.maxCharge();
        String chargeBy = awakening == null ? "" : value(awakening.chargeBy(), "");
        String awakeningDescription = awakening == null ? "" : value(awakening.description(), "");

        return new DivinePerkDefinition(
                perk,
                value(cfg.god(), "Déu desconegut"),
                value(cfg.shortDescription(), cfg.description()),
                breeds(cfg.allowedBreeds(), cfg.id()),
                maxCharge,
                chargeBy,
                awakeningDescription);
    }

    /**
     * Converteix regles JSON a regles internes.
     */
    private static List<Rule> rules(List<DivinePerkConfig.RuleConfig> configs) {
        if (configs == null || configs.isEmpty()) return List.of();
        return configs.stream()
                .filter(r -> r != null && !blank(r.type()))
                .map(r -> new Rule(r.type(), r.params() == null ? Map.of() : r.params()))
                .toList();
    }

    /**
     * Normalitza i afegeix tags.
     */
    private static Set<String> tags(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Set.of("DIVINE");
        Set<String> result = new LinkedHashSet<>();
        result.add("DIVINE");
        raw.stream()
                .filter(s -> !blank(s))
                .map(String::trim)
                .map(String::toUpperCase)
                .forEach(result::add);
        return Set.copyOf(result);
    }

    /**
     * Converteix noms de raça a enums.
     */
    private static List<Breed> breeds(List<String> raw, String id) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("La perk divina " + id + " no declara races permeses.");
        }
        return raw.stream()
                .filter(s -> !blank(s))
                .map(s -> enumValue(Breed.class, s.trim().toUpperCase(), id, "allowedBreeds"))
                .distinct()
                .toList();
    }

    /**
     * Converteix un string a enum validant errors.
     */
    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String id, String field) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Valor invàlid a " + id + " (" + field + "): " + raw, ex);
        }
    }

    /**
     * Retorna un valor o el per defecte si és buit.
     */
    private static String value(String value, String def) {
        return blank(value) ? def : value;
    }

    /**
     * Comprova si un text és nul o buit.
     */
    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
