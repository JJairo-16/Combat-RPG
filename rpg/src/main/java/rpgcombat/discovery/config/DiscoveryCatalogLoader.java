package rpgcombat.discovery.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;

/** Carrega el catàleg JSON de descobriments. */
public final class DiscoveryCatalogLoader {
    private static final Gson GSON = new Gson();

    private DiscoveryCatalogLoader() {}

    /** Carrega la configuració del catàleg des d'un fitxer JSON. */
    public static DiscoveryCatalogConfig load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            DiscoveryCatalogConfig config = GSON.fromJson(reader, DiscoveryCatalogConfig.class);
            return config == null ? new DiscoveryCatalogConfig(1, List.of(), List.of()) : config;
        }
    }
}
