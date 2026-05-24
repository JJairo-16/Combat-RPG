package rpgcombat.game.ui;

/**
 * Pantalla interactiva de la fitxa del jugador.
 */
public final class PlayerInfoScreen {
    private PlayerInfoScreen() {
    }

    /**
     * Mostra la fitxa dins d'una sessio interactiva independent.
     *
     * @param content fitxa ja renderitzada amb ANSI
     */
    public static void show(String content) {
        InteractiveTextScreen.show("Informació del jugador", content);
    }
}
