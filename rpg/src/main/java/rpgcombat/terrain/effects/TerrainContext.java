package rpgcombat.terrain.effects;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectState;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Context d'execució del motor genèric de terrenys.
 *
 * @param hit context del cop actual
 * @param phase fase que s'està resolent
 * @param rng generador aleatori de la fase
 * @param owner combatent que conté l'efecte
 * @param state estat intern de l'efecte
 */
record TerrainContext(HitContext hit, Phase phase, Random rng, Character owner, EffectState state) {
}
