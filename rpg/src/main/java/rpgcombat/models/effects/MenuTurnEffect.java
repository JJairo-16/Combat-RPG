package rpgcombat.models.effects;

import rpgcombat.models.characters.Character;

/**
 * Efectes que necessiten actualitzar-se una vegada per torn de menú/ronda, independentment del pipeline d'atac.
 */
public interface MenuTurnEffect {
    void onMenuTurnEnd(Character owner);
}
