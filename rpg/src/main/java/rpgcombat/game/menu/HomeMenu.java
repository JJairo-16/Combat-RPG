package rpgcombat.game.menu;

import java.util.List;

import rpgcombat.utils.interactive.SimpleMenu;

/** Menú principal mostrat abans de començar una partida. */
public final class HomeMenu {
    private HomeMenu() {
    }

    public enum Action {
        START,
        CREDITS,
        EXIT
    }

    /** Mostra el menú d'inici i retorna l'acció seleccionada. */
    public static Action show(String title) {
        int option = new SimpleMenu(1).getOption(
                List.of("Començar a jugar", "Crèdits", "Sortir"),
                title);

        return switch (option) {
            case 1 -> Action.START;
            case 2 -> Action.CREDITS;
            default -> Action.EXIT;
        };
    }
}
