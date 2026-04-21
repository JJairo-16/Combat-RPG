package rpgcombat.game.modifier.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import menu.action.MenuAction;
import rpgcombat.combat.models.Action;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.StatusModFactory;

import rpgcombat.models.characters.Character;

public final class StatusModLoader {
    private static final Gson GSON = new GsonBuilder().create();

    private static final Type MAP_TYPE = new TypeToken<Map<String, List<StatusModConfig>>>() {
    }.getType();

    private StatusModLoader() {
    }

    public static Map<String, List<StatusMod>> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, List<StatusModConfig>> raw = GSON.fromJson(reader, MAP_TYPE);
            return process(raw);
        }
    }

    public static Map<String, List<StatusMod>> load(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Map<String, List<StatusModConfig>> raw = GSON.fromJson(reader, MAP_TYPE);
            return process(raw);
        }
    }

    private static Map<String, List<StatusMod>> process(Map<String, List<StatusModConfig>> raw) {
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

    public static Map<String, List<StatusMod>> loadTest(Path path,
            Map<String, MenuAction<Action, Character>> customActions) throws IOException {
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
                                .map(cfg -> StatusModFactory.create(cfg, customActions, buildAlwaysTrue(customActions)))
                                .toList();

                result.put(effectKey, mods);
            }

            return Map.copyOf(result);
        }
    }

    private static Map<String, Predicate<Character>> buildAlwaysTrue(
            Map<String, MenuAction<Action, Character>> customActions) {

        Map<String, Predicate<Character>> map = new HashMap<>();

        for (String key : customActions.keySet()) {
            map.put(key, player -> true);
        }

        return map;
    }
}