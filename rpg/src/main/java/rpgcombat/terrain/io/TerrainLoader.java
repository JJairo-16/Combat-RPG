package rpgcombat.terrain.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import rpgcombat.terrain.model.TerrainDefinition;

/**
 * Carrega definicions de terreny des d'un fitxer JSON.
 */
public final class TerrainLoader {
    private static final Gson GSON = new Gson();

    private TerrainLoader() {
    }

    /**
     * Llegeix el catàleg de terrenys i hi afegeix sempre l'opció neutra.
     *
     * @param path ruta del fitxer de definicions
     * @return terrenys disponibles per a la partida
     * @throws IOException quan el fitxer existeix però no es pot llegir
     */
    public static List<TerrainDefinition> load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return List.of(TerrainDefinition.none());
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            TerrainDefinition[] loaded = GSON.fromJson(reader, TerrainDefinition[].class);
            List<TerrainDefinition> result = new ArrayList<>();
            result.add(TerrainDefinition.none());
            if (loaded != null) {
                for (TerrainDefinition terrain : loaded) {
                    if (terrain != null && !terrain.isNone()) {
                        result.add(terrain);
                    }
                }
            }
            return List.copyOf(result);
        }
    }
}
