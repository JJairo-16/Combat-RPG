package rpgcombat.terrain.model;

import java.util.Map;

/**
 * Bloc configurable de condició o acció d'un terreny.
 *
 * @param type tipus de regla que interpreta el motor
 * @param params paràmetres declarats al JSON
 */
public record TerrainRule(String type, Map<String, Object> params) {
    /**
     * Normalitza el tipus i protegeix els paràmetres rebuts.
     */
    public TerrainRule {
        type = type == null ? "" : type.trim().toUpperCase();
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
