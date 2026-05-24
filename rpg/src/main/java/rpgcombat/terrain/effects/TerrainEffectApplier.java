package rpgcombat.terrain.effects;

import rpgcombat.models.characters.Character;
import rpgcombat.terrain.model.TerrainDefinition;

/**
 * Aplica els efectes globals d'un terreny als dos combatents.
 */
public final class TerrainEffectApplier {
    private TerrainEffectApplier() {
    }

    /**
     * Afegeix el mateix efecte infinit de terreny a cada combatent disponible.
     *
     * @param terrain terreny seleccionat
     * @param player1 primer combatent
     * @param player2 segon combatent
     */
    public static void apply(TerrainDefinition terrain, Character player1, Character player2) {
        if (terrain == null || terrain.isNone()) {
            return;
        }
        if (player1 != null) {
            player1.addEffect(new ConfigurableTerrainEffect(terrain));
        }
        if (player2 != null) {
            player2.addEffect(new ConfigurableTerrainEffect(terrain));
        }
    }
}
