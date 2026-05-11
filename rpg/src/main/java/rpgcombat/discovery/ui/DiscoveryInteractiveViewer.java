package rpgcombat.discovery.ui;

import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.*;

import java.io.IOException;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.discovery.ui.models.DiscoveryOverview;
import rpgcombat.discovery.ui.render.DiscoveryRenderer;
import rpgcombat.discovery.ui.state.DiscoveryUiState;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.utils.ui.TerminalClear;

/**
 * Visor interactiu dels descobriments globals.
 *
 * <p>
 * Mostra les categories a l'esquerra, les entrades de la categoria al centre
 * i la fitxa de lectura a la dreta. El panell de detall no té cursor propi:
 * quan el jugador prem dreta des de la llista d'entrades, les categories es
 * pleguen per donar més espai a la lectura.
 * </p>
 */
public final class DiscoveryInteractiveViewer {
    private final DiscoveryUiState state = new DiscoveryUiState();
    private final DiscoveryRenderer renderer = new DiscoveryRenderer();

    /** Evita crear instàncies externes del visor. */
    private DiscoveryInteractiveViewer() {
    }

    /** Accions possibles del visor. */
    private enum Action {
        /** Mou la selecció cap amunt. */
        UP,

        /** Mou la selecció cap avall. */
        DOWN,

        /** Mou el focus cap a l'esquerra. */
        LEFT,

        /** Mou el focus cap a la dreta. */
        RIGHT,

        /** Activa el filtre següent. */
        NEXT_FILTER,

        /** Activa el filtre anterior. */
        PREVIOUS_FILTER,

        /** Surt del visor. */
        EXIT,

        /** Ignora l'entrada. */
        IGNORE
    }

    /** Mostra el visor o un avís si no hi ha catàleg. */
    public static void show(DiscoveryOverview overview) {
        if (overview == null) {
            TerminalClear.clearShared();
            Prettier.warn(NO_CATALOG);
            Menu.pause();
            return;
        }

        new DiscoveryInteractiveViewer().run(overview);
    }

    /** Executa el bucle principal del visor. */
    private void run(DiscoveryOverview overview) {
        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();
            BindingReader reader = new BindingReader(terminal.reader());
            KeyMap<Action> keys = keys(terminal);

            terminal.puts(Capability.cursor_invisible);
            TerminalClear.clear(terminal);

            while (true) {
                int width = Math.max(1, terminal.getWidth());
                int height = Math.max(1, terminal.getHeight());

                state.normalise(overview);

                terminal.writer().print("\033[H");
                terminal.writer().print(renderer.render(overview, state, width, height));
                terminal.flush();

                Action action = reader.readBinding(keys);
                if (action == null) {
                    continue;
                }

                switch (action) {
                    case UP -> state.moveUp();
                    case DOWN -> state.moveDown(overview);
                    case LEFT -> state.moveLeft();
                    case RIGHT -> state.moveRight();
                    case NEXT_FILTER -> state.nextFilter(overview);
                    case PREVIOUS_FILTER -> state.previousFilter(overview);
                    case IGNORE -> {
                    }
                    case EXIT -> {
                        terminal.writer().print(RESET);
                        terminal.puts(Capability.cursor_visible);
                        terminal.flush();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(OPEN_ERROR + e.getMessage());
        }
    }

    /** Defineix les tecles del visor. */
    private static KeyMap<Action> keys(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();

        map.bind(Action.UP, KEY_UP_1, KEY_UP_2, KEY_UP_3, KEY_UP_4);
        map.bind(Action.DOWN, KEY_DOWN_1, KEY_DOWN_2, KEY_DOWN_3, KEY_DOWN_4);
        map.bind(Action.LEFT, KEY_LEFT_1, KEY_LEFT_2, KEY_LEFT_3, KEY_LEFT_4);
        map.bind(Action.RIGHT, KEY_RIGHT_1, KEY_RIGHT_2, KEY_RIGHT_3, KEY_RIGHT_4, KEY_ENTER_CR, KEY_ENTER_LF);
        map.bind(Action.NEXT_FILTER, KEY_FILTER_NEXT);
        map.bind(Action.PREVIOUS_FILTER, KEY_FILTER_PREVIOUS);
        map.bind(Action.EXIT, KEY_EXIT_1, KEY_EXIT_2, KEY_ESCAPE);

        bindTerminalKey(map, terminal, Capability.key_up, Action.UP);
        bindTerminalKey(map, terminal, Capability.key_down, Action.DOWN);
        bindTerminalKey(map, terminal, Capability.key_left, Action.LEFT);
        bindTerminalKey(map, terminal, Capability.key_right, Action.RIGHT);

        return map;
    }

    /** Afegeix una tecla del terminal si existeix. */
    private static void bindTerminalKey(
            KeyMap<Action> map,
            Terminal terminal,
            Capability capability,
            Action action) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }
}
