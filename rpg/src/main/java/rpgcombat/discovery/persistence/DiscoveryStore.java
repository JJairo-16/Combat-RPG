package rpgcombat.discovery.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryKey;
import rpgcombat.discovery.DiscoveryProgress;
import rpgcombat.persistence.AppDataPaths;

/** Llegeix i desa el progrés global de descobriments a l'AppData del dispositiu. */
public final class DiscoveryStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;

    /** Resol la ruta real de desament dins AppData quan és relativa. */
    public Path resolveAppDataPath(String configuredPath) {
        return AppDataPaths.resolve(configuredPath, "discoveries.json");
    }

    /** Carrega el progrés desat, ignorant entrades desconegudes o obsoletes. */
    public Map<DiscoveryKey, DiscoveryProgress> load(Path path) {
        DiscoverySaveData save = readSave(path);
        Map<DiscoveryKey, DiscoveryProgress> result = new LinkedHashMap<>();
        if (save == null || save.discovered() == null) return result;

        for (DiscoverySavedEntry entry : save.discovered()) {
            DiscoveryCategory category = parseCategory(entry.category());
            if (category == null || entry.id() == null || entry.id().isBlank()) continue;
            DiscoveryKey key = new DiscoveryKey(category, entry.id());
            result.put(key, new DiscoveryProgress(key, parseInstant(entry.firstDiscoveredAt())));
        }
        return result;
    }

    /** Desa el progrés de manera segura. */
    public void save(Path path, Iterable<DiscoveryProgress> progress) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());

        List<DiscoverySavedEntry> entries = new ArrayList<>();
        if (progress != null) {
            for (DiscoveryProgress item : progress) {
                entries.add(new DiscoverySavedEntry(
                        item.key().category().name(),
                        item.key().id(),
                        item.firstDiscoveredAt().toString()));
            }
        }

        DiscoverySaveData data = new DiscoverySaveData(VERSION, entries);
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

    /** Llegeix el fitxer de desament si existeix. */
    private DiscoverySaveData readSave(Path path) {
        Path actualPath = readablePath(path);
        if (actualPath == null) return null;
        try (Reader reader = Files.newBufferedReader(actualPath, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, DiscoverySaveData.class);
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

    /** Converteix un text ISO-8601 en instant. */
    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Converteix text en una categoria vàlida. */
    private DiscoveryCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DiscoveryCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
