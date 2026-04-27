package rpgcombat.perks;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import rpgcombat.perks.PerkDefinition.Rule;
import rpgcombat.perks.config.PerkConfig;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Carrega perks des del JSON. */
public final class PerkLoader {
    private static final Gson GSON = new Gson();

    private PerkLoader() {}

    public static List<PerkDefinition> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            PerkConfig[] raw = GSON.fromJson(reader, PerkConfig[].class);
            if (raw == null) return List.of();

            List<PerkDefinition> result = new ArrayList<>(raw.length);
            for (PerkConfig cfg : raw) {
                if (cfg == null || cfg.id() == null || cfg.id().isBlank()) continue;
                result.add(toDefinition(cfg));
            }
            return List.copyOf(result);
        }
    }

    private static PerkDefinition toDefinition(PerkConfig cfg) {
        return new PerkDefinition(
                cfg.id(),
                value(cfg.name(), cfg.id()),
                value(cfg.description(), ""),
                enumValue(PerkFamily.class, value(cfg.family(), "STRATEGY"), cfg.id(), "família"),
                enumValue(Phase.class, value(cfg.trigger(), "AFTER_HIT"), cfg.id(), "trigger"),
                cfg.weight() == null ? 1 : Math.max(1, cfg.weight()),
                rules(cfg.conditions()),
                rules(cfg.actions()));
    }

    private static List<Rule> rules(List<PerkConfig.RuleConfig> configs) {
        if (configs == null || configs.isEmpty()) return List.of();
        return configs.stream()
                .filter(r -> r != null && r.type() != null && !r.type().isBlank())
                .map(r -> new Rule(r.type(), r.params() == null ? java.util.Map.of() : r.params()))
                .toList();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String perkId, String fieldName) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor invàlid a " + perkId + " (" + fieldName + "): " + raw, ex);
        }
    }

    private static String value(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
