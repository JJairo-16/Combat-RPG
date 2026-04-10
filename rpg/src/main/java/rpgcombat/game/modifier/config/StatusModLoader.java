package rpgcombat.game.modifier.config;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import menu.action.MenuAction;
import rpgcombat.combat.Action;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.StatusModFactory;

import rpgcombat.models.characters.Character;

public final class StatusModLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Type MAP_TYPE =
        new TypeToken<Map<String, List<StatusModConfig>>>() {}.getType();

    private StatusModLoader() {}

    public static Map<String, List<StatusMod>> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, List<StatusModConfig>> raw = GSON.fromJson(reader, MAP_TYPE);

            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }

            Map<String, List<StatusMod>> result = new LinkedHashMap<>();

            for (Map.Entry<String, List<StatusModConfig>> entry : raw.entrySet()) {
                String effectKey = entry.getKey();
                List<StatusModConfig> configs = entry.getValue();

                List<StatusMod> mods = (configs == null)
                    ? List.of()
                    : configs.stream()
                        .map(StatusModFactory::create)
                        .toList();

                result.put(effectKey, mods);
            }

            return Map.copyOf(result);
        }
    }

    public static Map<String, List<StatusMod>> loadTest(Path path, Map<String, MenuAction<Action, Character>> customActions) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, List<StatusModConfig>> raw = GSON.fromJson(reader, MAP_TYPE);

            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }

            Map<String, List<StatusMod>> result = new LinkedHashMap<>();

            for (Map.Entry<String, List<StatusModConfig>> entry : raw.entrySet()) {
                String effectKey = entry.getKey();
                List<StatusModConfig> configs = entry.getValue();

                List<StatusMod> mods = (configs == null)
                    ? List.of()
                    : configs.stream()
                        .map(cfg -> StatusModFactory.create(cfg, customActions))
                        .toList();

                result.put(effectKey, mods);
            }

            return Map.copyOf(result);
        }
    }
}