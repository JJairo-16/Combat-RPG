package rpgcombat.gamemode.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.gamemode.model.GameModeDefinition;

/** Registre global dels modes de joc carregats. */
public final class GameModeRegistry {
    private static final String NORMAL_ID = "NORMAL";
    private static final Map<String, GameModeDefinition> BY_ID = new LinkedHashMap<>();
    private static boolean loaded;

    static {
        initialize(defaultModes());
    }

    private GameModeRegistry() {
    }

    public static synchronized void initialize(Collection<GameModeDefinition> modes) {
        List<GameModeDefinition> effective = modes == null || modes.isEmpty()
                ? defaultModes()
                : List.copyOf(modes);

        Map<String, GameModeDefinition> map = new LinkedHashMap<>();
        for (GameModeDefinition mode : effective) {
            if (mode == null) {
                continue;
            }
            GameModeDefinition previous = map.put(mode.id(), mode);
            if (previous != null) {
                throw new IllegalArgumentException("Mode de joc repetit: " + mode.id());
            }
        }

        if (!map.containsKey(NORMAL_ID)) {
            map.put(NORMAL_ID, GameModeDefinition.normal());
        }

        BY_ID.clear();
        BY_ID.putAll(map);
        loaded = true;
    }

    public static List<GameModeDefinition> defaultModes() {
        return List.of(GameModeDefinition.normal(), GameModeDefinition.beginner());
    }

    public static List<GameModeDefinition> all() {
        ensureLoaded();
        return List.copyOf(BY_ID.values());
    }

    public static List<GameModeDefinition> unlocked(AchievementSystem achievements, DiscoverySystem discoveries) {
        ensureLoaded();
        List<GameModeDefinition> modes = BY_ID.values().stream()
                .filter(mode -> mode.isUnlocked(achievements, discoveries))
                .toList();
        return modes.isEmpty() ? List.of(getOrDefault(NORMAL_ID)) : modes;
    }

    public static Optional<GameModeDefinition> find(String id) {
        ensureLoaded();
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.trim().toUpperCase()));
    }

    public static GameModeDefinition getOrDefault(String id) {
        ensureLoaded();
        return find(id).orElseGet(() -> BY_ID.getOrDefault(NORMAL_ID, GameModeDefinition.normal()));
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            initialize(defaultModes());
        }
    }
}
