package rpgcombat.config.app;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import rpgcombat.config.debug.DebugProfile;

/** Carrega la configuració de l'aplicació des d'un fitxer JSON. */
public final class AppConfigLoader {
    private static final Gson GSON = new Gson();

    private AppConfigLoader() {
    }

    /**
     * Carrega la configuració des del path indicat.
     *
     * @param path ruta del fitxer de configuració
     * @return configuració carregada o per defecte si falla
     * @throws IOException si hi ha errors de lectura o parseig
     */
    public static AppConfig load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (element == null || element.isJsonNull()) {
                return defaultConfig();
            }

            if (!element.isJsonObject()) {
                throw new IOException("La configuració ha de ser un objecte JSON");
            }

            JsonObject json = element.getAsJsonObject();

            boolean useProfile = getBoolean(json, "useProfile", false);

            if (useProfile) {
                return AppConfigProfiles.from(getProfile(json));
            }

            AppConfig config = GSON.fromJson(json, AppConfig.class);
            return config != null ? config : defaultConfig();

        } catch (JsonParseException e) {
            throw new IOException("Error en analitzar la configuració a " + path, e);
        }
    }

    /** Retorna la configuració per defecte. */
    public static AppConfig defaultConfig() {
        return AppConfigProfiles.normal();
    }

    /** Llegeix un booleà del JSON amb valor per defecte. */
    private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsBoolean()
                : defaultValue;
    }

    /** Llegeix el perfil de debug del JSON. */
    private static DebugProfile getProfile(JsonObject json) {
        if (!json.has("profile") || json.get("profile").isJsonNull()) {
            return null;
        }
        return GSON.fromJson(json.get("profile"), DebugProfile.class);
    }
}