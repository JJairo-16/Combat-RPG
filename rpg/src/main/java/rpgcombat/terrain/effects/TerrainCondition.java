package rpgcombat.terrain.effects;

/**
 * Condició executable del motor genèric de terrenys.
 */
@FunctionalInterface
interface TerrainCondition {
    /**
     * Avalua la condició sobre el context actiu.
     *
     * @param context context de la regla
     * @return {@code true} quan la regla pot continuar
     */
    boolean matches(TerrainContext context);
}
