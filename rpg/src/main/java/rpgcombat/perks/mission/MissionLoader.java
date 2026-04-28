package rpgcombat.perks.mission;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import rpgcombat.combat.models.Action;
import rpgcombat.perks.config.MissionConfig;

/**
 * Carrega i transforma missions definides en JSON a objectes de domini.
 */
public final class MissionLoader {
    private static final Gson GSON = new Gson();

    private MissionLoader() {}

    /**
     * Llegeix un fitxer JSON i construeix la llista de missions.
     *
     * @param path ruta del fitxer
     * @return llista de missions
     * @throws IOException si falla la lectura
     */
    public static List<MissionDefinition> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            MissionConfig[] raw = GSON.fromJson(reader, MissionConfig[].class);
            if (raw == null) return List.of();

            List<MissionDefinition> result = new ArrayList<>(raw.length);
            for (MissionConfig cfg : raw) {
                if (cfg == null || cfg.objective() == null) continue;
                result.add(toDefinition(cfg));
            }
            return List.copyOf(result);
        }
    }

    /**
     * Converteix una configuració JSON en una definició de missió.
     */
    private static MissionDefinition toDefinition(MissionConfig cfg) {
        MissionConfig.ObjectiveConfig obj = cfg.objective();
        String id = value(cfg.id(), value(cfg.name(), "MISSION"));
        ObjectiveType type = enumValue(ObjectiveType.class, value(obj.type(), "COUNT_EVENT"), id, "tipus d'objectiu");
        List<Action> sequence = actions(obj.sequence(), id);
        double target = obj.target() == null ? defaultTarget(type, sequence) : Math.max(1.0, obj.target());

        return new MissionDefinition(
                id,
                value(cfg.name(), cfg.id()),
                value(cfg.description(), ""),
                cfg.weight() == null ? 1 : Math.max(1, cfg.weight()),
                type,
                event(obj.event(), id),
                event(obj.successEvent(), id),
                event(obj.resetEvent(), id),
                sequence,
                target,
                obj.value() == null ? 0.0 : obj.value(),
                obj.turns() == null ? 1 : Math.max(1, obj.turns()));
    }

    /** Retorna l'objectiu per defecte d'una missió. */
    private static double defaultTarget(ObjectiveType type, List<Action> sequence) {
        if (type == ObjectiveType.ACTION_SEQUENCE && sequence != null && !sequence.isEmpty()) {
            return sequence.size();
        }
        return 1.0;
    }

    /** Converteix un text a esdeveniment de missió. */
    private static MissionEvent event(String raw, String missionId) {
        return raw == null || raw.isBlank() ? null : enumValue(MissionEvent.class, raw, missionId, "esdeveniment");
    }

    /** Converteix una llista de textos a accions. */
    private static List<Action> actions(List<String> raw, String missionId) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(action -> enumValue(Action.class, action, missionId, "acció")).toList();
    }

    /**
     * Converteix un valor a enum validant-lo.
     *
     * @throws IllegalArgumentException si el valor no és vàlid
     */
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