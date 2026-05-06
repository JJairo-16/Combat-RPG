package rpgcombat.achievements.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rpgcombat.achievements.AchievementProgress;
import rpgcombat.achievements.config.AchievementDefinition;

/** Llegeix i desa el progrés global d'assoliments a l'AppData del dispositiu. */
public final class AchievementStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;

    /**
     * Resol la ruta real de desament. Les rutes relatives es guarden sempre dins
     * l'AppData del sistema, no dins del projecte.
     */
    public Path resolveAppDataPath(String configuredPath) {
        String file = configuredPath == null || configuredPath.isBlank() ? "achievements.json" : configuredPath;
        Path path = Path.of(file);
        if (path.isAbsolute()) return path;
        return appDataDirectory().resolve(path).normalize();
    }

    /** Carrega el progrés, creant entrades buides per als assoliments nous. */
    public Map<String, AchievementProgress> load(Path path, Collection<AchievementDefinition> definitions) {
        AchievementSaveData save = readSave(path);
        Map<String, AchievementSavedProgress> saved = save == null || save.achievements() == null
                ? Map.of()
                : save.achievements();

        Map<String, AchievementProgress> result = new LinkedHashMap<>();
        if (definitions == null) return result;

        for (AchievementDefinition definition : definitions) {
            AchievementSavedProgress item = saved.get(definition.id());
            if (item == null) {
                result.put(definition.id(), new AchievementProgress(definition));
                continue;
            }

            result.put(definition.id(), new AchievementProgress(
                    definition,
                    item.progress(),
                    item.sequenceIndex(),
                    item.completed(),
                    parseInstant(item.completedAt()),
                    item.valueProgress(),
                    item.actorSequenceProgress()));
        }

        return result;
    }

    /** Desa el progrés de manera segura. */
    public void save(Path path, Collection<AchievementProgress> progress) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());

        Map<String, AchievementSavedProgress> items = new LinkedHashMap<>();
        if (progress != null) {
            for (AchievementProgress item : progress) {
                items.put(item.definition().id(), new AchievementSavedProgress(
                        item.progress(),
                        item.sequenceIndex(),
                        item.completed(),
                        item.completedAt() == null ? null : item.completedAt().toString(),
                        item.valueProgress(),
                        item.actorSequenceProgress()));
            }
        }

        AchievementSaveData data = new AchievementSaveData(VERSION, items);
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicError) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Llegeix el fitxer si existeix. */
    private AchievementSaveData readSave(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, AchievementSaveData.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Converteix una data ISO a Instant. */
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Retorna el directori AppData adequat per al sistema operatiu. */
    private Path appDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "RPGCombat");
        }

        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "rpgcombat");
        }

        return Path.of(System.getProperty("user.home", "."), ".rpgcombat");
    }
}
