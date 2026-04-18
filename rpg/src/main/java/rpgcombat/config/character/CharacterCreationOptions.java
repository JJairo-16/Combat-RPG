package rpgcombat.config.character;

/**
 * Opcions de creació d'un personatge.
 */
public record CharacterCreationOptions(
        CharacterCreationMode mode) {

    public CharacterCreationOptions {
        if (mode == null) {
            mode = CharacterCreationMode.NORMAL;
        }
    }
}
