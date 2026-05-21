package rpgcombat.terrain.effects;

import rpgcombat.models.effects.EffectResult;

/**
 * Acció executable del motor genèric de terrenys.
 */
@FunctionalInterface
interface TerrainAction {
    /**
     * Aplica l'acció sobre el context actiu.
     *
     * @param context context de la regla
     * @return resultat de l'efecte
     */
    EffectResult apply(TerrainContext context);
}
