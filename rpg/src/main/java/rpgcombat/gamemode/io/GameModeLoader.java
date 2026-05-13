package rpgcombat.gamemode.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;

/** Carrega modes de joc des d'un JSON extern. */
public final class GameModeLoader {
    private static final Gson GSON = new Gson();

    private GameModeLoader() {
    }

    public static List<GameModeDefinition> load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            GameModeConfig[] configs = GSON.fromJson(reader, GameModeConfig[].class);
            if (configs == null || configs.length == 0) {
                return GameModeRegistry.defaultModes();
            }

            List<GameModeDefinition> modes = new ArrayList<>(configs.length);
            for (GameModeConfig config : configs) {
                modes.add(GameModeMapper.toDefinition(config));
            }
            return List.copyOf(modes);
        }
    }
}
