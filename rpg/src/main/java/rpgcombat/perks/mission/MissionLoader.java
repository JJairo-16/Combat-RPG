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

/** Carrega missions des del JSON. */
public final class MissionLoader {
    private static final Gson GSON = new Gson();

    private MissionLoader() {}

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

    private static MissionDefinition toDefinition(MissionConfig cfg) {
        MissionConfig.ObjectiveConfig obj = cfg.objective();
        String id = value(cfg.id(), value(cfg.name(), "MISSION"));
        return new MissionDefinition(
                id,
                value(cfg.name(), cfg.id()),
                value(cfg.description(), ""),
                cfg.weight() == null ? 1 : Math.max(1, cfg.weight()),
                enumValue(ObjectiveType.class, value(obj.type(), "COUNT_EVENT"), id, "tipus d'objectiu"),
                event(obj.event(), id),
                event(obj.successEvent(), id),
                event(obj.resetEvent(), id),
                actions(obj.sequence(), id),
                obj.target() == null ? 1.0 : obj.target(),
                obj.value() == null ? 0.0 : obj.value(),
                obj.turns() == null ? 1 : Math.max(1, obj.turns()));
    }

    private static MissionEvent event(String raw, String missionId) {
        return raw == null || raw.isBlank() ? null : enumValue(MissionEvent.class, raw, missionId, "esdeveniment");
    }

    private static List<Action> actions(List<String> raw, String missionId) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(action -> enumValue(Action.class, action, missionId, "acció")).toList();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String ownerId, String fieldName) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor invàlid a " + ownerId + " (" + fieldName + "): " + raw, ex);
        }
    }

    private static String value(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
