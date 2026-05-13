package rpgcombat.achievements.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import rpgcombat.achievements.AchievementConfig;
import rpgcombat.achievements.AchievementEvent;
import rpgcombat.combat.models.Action;

/** Carrega assoliments definits en JSON. */
public final class AchievementLoader {
    private static final Gson GSON = new Gson();

    private AchievementLoader() {}

    /**
     * Llegeix un fitxer JSON i retorna les definicions d'assoliments.
     *
     * @param path ruta del fitxer de definicions
     * @return llista d'assoliments
     * @throws IOException si falla la lectura
     */
    public static List<AchievementDefinition> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            AchievementConfig[] raw = GSON.fromJson(reader, AchievementConfig[].class);
            if (raw == null) return List.of();

            List<AchievementDefinition> result = new ArrayList<>(raw.length);
            Set<String> ids = new HashSet<>();

            for (AchievementConfig cfg : raw) {
                if (cfg == null || cfg.objective() == null) continue;
                AchievementDefinition definition = toDefinition(cfg);
                if (!ids.add(definition.id())) {
                    throw new IllegalArgumentException("Assoliment duplicat: " + definition.id());
                }
                result.add(definition);
            }

            return List.copyOf(result);
        }
    }

    /** Converteix una configuració JSON en una definició validada. */
    private static AchievementDefinition toDefinition(AchievementConfig cfg) {
        String id = value(cfg.id(), value(cfg.name(), "ACHIEVEMENT"));
        AchievementConfig.ObjectiveConfig obj = cfg.objective();
        AchievementObjectiveType type = enumValue(AchievementObjectiveType.class, value(obj.type(), "COUNT_EVENT"), id,
                "tipus d'objectiu");
        List<Action> sequence = actions(obj.sequence(), id);
        List<String> requiredValues = obj.requiredValues() == null ? List.of() : List.copyOf(obj.requiredValues());
        double target = obj.target() == null ? defaultTarget(type, sequence, requiredValues, cfg.goal()) : Math.max(1.0, obj.target());
        int goal = cfg.goal() == null ? (int) Math.ceil(target) : Math.max(1, cfg.goal());

        return new AchievementDefinition(
                id,
                value(cfg.name(), id),
                value(cfg.description(), ""),
                goal,
                visibility(cfg.visibility()),
                new AchievementObjective(
                        type,
                        event(obj.event(), id),
                        event(obj.successEvent(), id),
                        event(obj.resetEvent(), id),
                        sequence,
                        target,
                        obj.value() == null ? 0.0 : obj.value(),
                        value(obj.valueField(), null),
                        value(obj.uniqueField(), null),
                        requiredValues,
                        obj.targetPerValue() == null ? 1.0 : Math.max(1.0, obj.targetPerValue()),
                        conditions(obj.conditions())));
    }

    /** Calcula l'objectiu per defecte. */
    private static double defaultTarget(AchievementObjectiveType type, List<Action> sequence, List<String> requiredValues, Integer goal) {
        if (type == AchievementObjectiveType.ACTION_SEQUENCE && sequence != null && !sequence.isEmpty()) {
            return sequence.size();
        }
        if ((type == AchievementObjectiveType.ALL_UNIQUE_VALUES || type == AchievementObjectiveType.EACH_UNIQUE_VALUE_COUNT)
                && requiredValues != null && !requiredValues.isEmpty()) {
            return requiredValues.size();
        }
        return goal == null ? 1.0 : Math.max(1, goal);
    }

    /** Converteix la visibilitat JSON en domini. */
    private static AchievementVisibility visibility(AchievementConfig.VisibilityConfig cfg) {
        if (cfg == null) return AchievementVisibility.visible();
        boolean showName = cfg.showNameBeforeComplete() == null || cfg.showNameBeforeComplete();
        boolean showDescription = cfg.showDescriptionBeforeComplete() == null || cfg.showDescriptionBeforeComplete();
        return new AchievementVisibility(showName, showDescription);
    }

    /** Converteix condicions JSON en domini. */
    private static List<AchievementCondition> conditions(List<AchievementConfig.ConditionConfig> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .filter(c -> c != null && c.field() != null && !c.field().isBlank())
                .map(c -> new AchievementCondition(c.field(), value(c.operator(), "=="), String.valueOf(c.value())))
                .toList();
    }

    /** Converteix un text a esdeveniment. */
    private static AchievementEvent event(String raw, String achievementId) {
        return raw == null || raw.isBlank() ? null
                : enumValue(AchievementEvent.class, raw, achievementId, "esdeveniment");
    }

    /** Converteix una llista de textos a accions. */
    private static List<Action> actions(List<String> raw, String achievementId) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(action -> enumValue(Action.class, action, achievementId, "acció")).toList();
    }

    /** Converteix un valor a enum validant-lo. */
    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String ownerId, String fieldName) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor invàlid a " + ownerId + " (" + fieldName + "): " + raw, ex);
        }
    }

    /** Retorna un valor o un per defecte si és buit. */
    private static String value(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
