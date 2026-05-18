package rpgcombat.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import rpgcombat.persistence.AppDataPaths;

/** Llegeix i desa els ajustos de l'usuari a l'AppData del dispositiu. */
public final class UserSettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path defaultsPath;
    private final Path savePath;

    /** Crea un magatzem apuntant al fitxer d'AppData. */
    public UserSettingsStore(String defaultsConfigPath, String configuredSavePath) {
        this(
                Path.of(defaultsConfigPath),
                AppDataPaths.resolve(configuredSavePath, "settings.json"));
    }

    /** Crea un magatzem amb una ruta concreta, pensat per a proves. */
    UserSettingsStore(Path defaultsPath, Path savePath) {
        this.defaultsPath = defaultsPath;
        this.savePath = savePath == null ? AppDataPaths.resolve(null, "settings.json") : savePath;
    }

    /** Resol la ruta real del fitxer d'ajustos. */
    public Path resolveAppDataPath() {
        return savePath;
    }

    /** Carrega els ajustos desats sobre els valors per defecte del joc. */
    public UserSettings load() {
        UserSettings defaults = readSettings(defaultsPath, UserSettings.defaults());
        return readSettings(savePath, defaults);
    }

    /** Desa els ajustos de manera segura. */
    public void save(UserSettings settings) throws IOException {
        Path savePath = resolveAppDataPath();
        UserSettings safeSettings = settings == null ? UserSettings.defaults() : settings;
        if (savePath.getParent() != null) {
            Files.createDirectories(savePath.getParent());
        }

        Path tmp = savePath.resolveSibling(savePath.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(SettingsData.from(safeSettings), writer);
        }

        try {
            Files.move(tmp, savePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicError) {
            Files.move(tmp, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private UserSettings readSettings(Path path, UserSettings fallback) {
        UserSettings safeFallback = fallback == null ? UserSettings.defaults() : fallback;
        if (path == null || !Files.exists(path)) {
            return safeFallback;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                return safeFallback;
            }
            return merge(element.getAsJsonObject(), safeFallback);
        } catch (Exception ignored) {
            return safeFallback;
        }
    }

    private UserSettings merge(JsonObject json, UserSettings fallback) {
        return new UserSettings(
                getBoolean(json, "showMomentumMessages", fallback.showMomentumMessages()));
    }

    private boolean getBoolean(JsonObject json, String key, boolean fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        return json.get(key).getAsBoolean();
    }

    private record SettingsData(boolean showMomentumMessages) {
        private static SettingsData from(UserSettings settings) {
            return new SettingsData(settings.showMomentumMessages());
        }
    }
}
