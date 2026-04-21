package rpgcombat.config.debug;

import rpgcombat.models.characters.Character;

/** Utilitats de depuració aplicades en temps d'execució. */
public final class DebugRuntime {
    private DebugRuntime() {
    }

   /** Aplica la configuració de debug al personatge. */
    public static void applyDebugOptions(Character character, DebugOptions options) {
        if (character == null || options == null) {
            return;
        }

        character.setInvulnerable(options.forceInvulnerability());
    }
}
