package rpgcombat.game.ui;

/**
 * Pantalla interactiva del final del combat.
 */
final class VictoryScreen {
    private VictoryScreen() {
    }

    static void show(String content) {
        InteractiveTextScreen.show("Fi del combat", content);
    }
}
