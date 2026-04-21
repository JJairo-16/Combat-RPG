package rpgcombat.config.character;

/** Configuració de creació de personatges. */
public record CharactersConfig(
        CharacterCreationMode player1,
        CharacterCreationMode player2) {

    public CharactersConfig {
        if (player1 == null) {
            player1 = CharacterCreationMode.NORMAL;
        }
        if (player2 == null) {
            player2 = CharacterCreationMode.NORMAL;
        }
    }

    public static CharactersConfig defaultConfig() {
        return new CharactersConfig(CharacterCreationMode.NORMAL, CharacterCreationMode.NORMAL);
    }

    public static CharactersConfig debugConfig() {
        return new CharactersConfig(CharacterCreationMode.DEBUG, CharacterCreationMode.DEBUG);
    }
}
