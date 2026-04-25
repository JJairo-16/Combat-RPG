package rpgcombat.game.menu;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.game.EndGameAction;
import rpgcombat.utils.interactive.SimpleMenu;

/** Menú mostrat quan acaba una partida. */
public final class EndGameMenu {
    private EndGameMenu() {
    }

    /** Demana què fer després de la partida. */
    public static EndGameAction ask(boolean allowHome) {
        List<String> options = new ArrayList<>();
        options.add("Tornar a jugar");

        if (allowHome) {
            options.add("Tornar a la pantalla d'inici");
        }

        options.add("Sortir");

        int option = new SimpleMenu(1).getOption(options, "Fi de la partida");

        if (option == 1) {
            return EndGameAction.PLAY_AGAIN;
        }

        if (allowHome && option == 2) {
            return EndGameAction.HOME;
        }

        return EndGameAction.EXIT;
    }
}
