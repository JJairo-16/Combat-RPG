package rpgcombat.config.app;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Carrega la configuració de l'aplicació des d'un JSON.
 */
public final class AppConfigLoader {
    private static final Gson GSON = new GsonBuilder().create();

    private AppConfigLoader() {
    }

    public static AppConfig load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            AppConfig raw = GSON.fromJson(reader, AppConfig.class);
            if (raw == null) {
                return defaultConfig();
            }

            if (raw.useProfile()) {
                return AppConfigProfiles.from(raw.profile());
            }

            return raw;
        }
    }

    public static AppConfig defaultConfig() {
        return AppConfigProfiles.normal();
    }
}
