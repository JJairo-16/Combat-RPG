package rpgcombat.achievements.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registre global de definicions d'assoliments. */
public final class AchievementRegistry {
    private static Map<String, AchievementDefinition> definitions = Map.of();

    private AchievementRegistry() {}

    /** Inicialitza el registre amb definicions carregades. */
    public static void initialize(List<AchievementDefinition> loaded) {
        Map<String, AchievementDefinition> map = new LinkedHashMap<>();
        if (loaded != null) {
            for (AchievementDefinition definition : loaded) {
                if (definition != null) map.put(definition.id(), definition);
            }
        }
        definitions = Map.copyOf(map);
    }

    /** @return totes les definicions registrades. */
    public static Collection<AchievementDefinition> all() {
        return definitions.values();
    }

    /** Cerca una definició per id. */
    public static Optional<AchievementDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
}
