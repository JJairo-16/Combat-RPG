package rpgcombat.terrain.registry;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import rpgcombat.terrain.model.TerrainDefinition;

/**
 * Registre global dels terrenys carregats durant l'arrencada.
 */
public final class TerrainRegistry {
    private static List<TerrainDefinition> terrains = List.of(TerrainDefinition.none());

    private TerrainRegistry() {
    }

    /**
     * Substitueix el catàleg registrat.
     *
     * @param loaded definicions carregades des de disc
     */
    public static void initialize(List<TerrainDefinition> loaded) {
        terrains = loaded == null || loaded.isEmpty() ? List.of(TerrainDefinition.none()) : List.copyOf(loaded);
    }

    /**
     * Retorna totes les definicions registrades.
     *
     * @return catàleg immutable de terrenys
     */
    public static List<TerrainDefinition> all() {
        return terrains;
    }

    /**
     * Resol l'opció neutra del catàleg.
     *
     * @return terreny sense efectes
     */
    public static TerrainDefinition none() {
        return find(TerrainDefinition.NONE_ID).orElse(TerrainDefinition.none());
    }

    /**
     * Cerca un terreny pel seu identificador.
     *
     * @param id identificador a cercar
     * @return terreny trobat, si existeix
     */
    public static Optional<TerrainDefinition> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String key = id.trim().toUpperCase();
        return terrains.stream().filter(terrain -> terrain.id().equals(key)).findFirst();
    }

    /**
     * Tria aleatòriament un terreny que tingui efectes.
     *
     * @param rng generador aleatori opcional
     * @return terreny jugable o l'opció neutra si no n'hi ha cap
     */
    public static TerrainDefinition randomPlayable(Random rng) {
        List<TerrainDefinition> playable = terrains.stream()
                .filter(terrain -> !terrain.isNone())
                .toList();
        if (playable.isEmpty()) {
            return none();
        }
        Random safeRng = rng == null ? new Random() : rng;
        return playable.get(safeRng.nextInt(playable.size()));
    }
}
