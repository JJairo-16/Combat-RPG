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
import rpgcombat.persistence.AppDataPaths;

/** Llegeix i desa el progrés global d'assoliments a l'AppData del dispositiu. */
public final class AchievementStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;

    /**
     * Resol la ruta real de desament. Les rutes relatives es guarden sempre dins
     * l'AppData del sistema, no dins del projecte.
     */
    public Path resolveAppDataPath(String configuredPath) {
        return AppDataPaths.resolve(configuredPath, "achievements.json");
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
                    item.progress() == null ? 0.0 : item.progress(),
                    item.sequenceIndex() == null ? 0 : item.sequenceIndex(),
                    Boolean.TRUE.equals(item.completed()),
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
                items.put(item.definition().id(), savedProgress(item));
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
        Path actualPath = readablePath(path);
        if (actualPath == null) return null;
        try (Reader reader = Files.newBufferedReader(actualPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, AchievementSaveData.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Tria la ruta nova o, si encara no existeix, l'equivalent anterior. */
    private Path readablePath(Path path) {
        if (path == null) return null;
        if (Files.exists(path)) return path;
        Path legacy = AppDataPaths.legacyEquivalent(path);
        return legacy != null && Files.exists(legacy) ? legacy : null;
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

    /** Compacta assoliments completats i limita decimals del progrés pendent. */
    private AchievementSavedProgress savedProgress(AchievementProgress item) {
        if (item.completed()) {
            return AchievementSavedProgress.completed(item.completedAt() == null ? null : item.completedAt().toString());
        }

        return AchievementSavedProgress.pending(
                round2(item.progress()),
                item.sequenceIndex(),
                roundValues(item.valueProgress()),
                item.actorSequenceProgress());
    }

    private Map<String, Double> roundValues(Map<String, Double> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> rounded = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                rounded.put(key, round2(value));
            }
        });
        return rounded;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
