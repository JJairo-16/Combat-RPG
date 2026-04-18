package rpgcombat.config.character;

/**
 * Configuració de creació de personatges.
 */
public record CharactersConfig(
        CharacterCreationOptions player1,
        CharacterCreationOptions player2) {

    public CharactersConfig {
        if (player1 == null) {
            player1 = new CharacterCreationOptions(CharacterCreationMode.NORMAL);
        }
        if (player2 == null) {
            player2 = new CharacterCreationOptions(CharacterCreationMode.NORMAL);
        }
    }
}
