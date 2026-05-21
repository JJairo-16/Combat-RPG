package rpgcombat.terrain.ui;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import rpgcombat.terrain.model.TerrainDefinition;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalInput;
import rpgcombat.utils.terminal.TerminalSession;
import rpgcombat.utils.ui.TerminalClear;

import static rpgcombat.utils.ui.Ansi.RESET;

/**
 * Selector interactiu de terreny amb informació lateral i graella navegable.
 */
public final class TerrainSelectionScreen {
    private TerrainSelectionScreen() {
    }

    /**
     * Mostra els terrenys disponibles i retorna l'opció escollida.
     *
     * @param terrains terrenys disponibles
     * @return terreny seleccionat o el terreny neutre quan es cancel·la
     */
    public static TerrainDefinition choose(List<TerrainDefinition> terrains) {
        if (terrains == null || terrains.isEmpty()) {
            return TerrainDefinition.none();
        }

        List<TerrainDefinition> options = terrains.stream().filter(Objects::nonNull).toList();
        if (options.isEmpty()) {
            return TerrainDefinition.none();
        }

        try (TerminalSession session = SharedTerminal.openSession()) {
            return chooseInteractive(options, session.terminal());
        } catch (IOException ex) {
            return TerrainDefinition.none();
        }
    }

    /**
     * Executa el bucle interactiu de selecció dins del terminal compartit.
     *
     * @param options terrenys que es poden triar
     * @param terminal terminal actiu
     * @return terreny seleccionat o terreny neutre quan es cancel·la
     */
    private static TerrainDefinition chooseInteractive(List<TerrainDefinition> options, Terminal terminal) {
        BindingReader reader = new BindingReader(terminal.reader());
        KeyMap<Action> keys = keys(terminal);
        TerrainSelectionRenderer renderer = new TerrainSelectionRenderer();
        TerrainSelectionRenderer.View lastView = null;
        int selected = 0;
        int lastSelected = -1;

        terminal.puts(Capability.cursor_invisible);
        TerminalClear.clear(terminal);

        try {
            while (true) {
                TerrainSelectionRenderer.View view = renderer.view(terminal, options.size());
                if (!view.equals(lastView)) {
                    renderer.renderFull(terminal, options, selected, view);
                    lastView = view;
                    lastSelected = selected;
                } else if (selected != lastSelected) {
                    renderer.redrawSelection(terminal, options, lastSelected, selected, view);
                    lastSelected = selected;
                }
                terminal.flush();

                Action action = TerminalInput.readBindingIgnoringMouse(reader, keys, terminal, Action.IGNORE);
                if (action == null) {
                    renderer.renderFull(terminal, options, selected, view);
                    terminal.flush();
                    continue;
                }

                switch (action) {
                    case LEFT -> selected = moveLeft(selected);
                    case RIGHT -> selected = moveRight(selected, options.size());
                    case UP -> selected = moveUp(selected, view.columns());
                    case DOWN -> selected = moveDown(selected, view.columns(), options.size());
                    case SELECT -> {
                        return options.get(selected);
                    }
                    case CANCEL -> {
                        return TerrainDefinition.none();
                    }
                    case IGNORE -> {
                    }
                }
            }
        } finally {
            terminal.writer().print(RESET);
            terminal.puts(Capability.cursor_visible);
            TerminalClear.clear(terminal);
            terminal.flush();
        }
    }

    /**
     * Construeix el mapa de tecles de navegació del selector.
     *
     * @param terminal terminal actiu
     * @return mapatge d'accions interactives
     */
    private static KeyMap<Action> keys(Terminal terminal) {
        KeyMap<Action> map = new KeyMap<>();
        map.bind(Action.LEFT, "a", "A");
        map.bind(Action.RIGHT, "d", "D");
        map.bind(Action.UP, "w", "W");
        map.bind(Action.DOWN, "s", "S");
        map.bind(Action.SELECT, "\r", "\n");
        map.bind(Action.CANCEL, "q", "Q", "\033");

        bind(map, terminal, Capability.key_left, Action.LEFT);
        bind(map, terminal, Capability.key_right, Action.RIGHT);
        bind(map, terminal, Capability.key_up, Action.UP);
        bind(map, terminal, Capability.key_down, Action.DOWN);
        TerminalInput.bindMouseIgnore(map, terminal, Action.IGNORE);
        return map;
    }

    /**
     * Vincula una capacitat de terminal a l'acció indicada quan existeix.
     *
     * @param map mapa de tecles
     * @param terminal terminal actiu
     * @param capability capacitat de cursor
     * @param action acció associada
     */
    private static void bind(KeyMap<Action> map, Terminal terminal, Capability capability, Action action) {
        String key = KeyMap.key(terminal, capability);
        if (key != null) {
            map.bind(action, key);
        }
    }

    /**
     * Mou la selecció una posició cap a l'esquerra.
     *
     * @param selected índex actual
     * @return índex nou
     */
    private static int moveLeft(int selected) {
        return Math.max(0, selected - 1);
    }

    /**
     * Mou la selecció una posició cap a la dreta.
     *
     * @param selected índex actual
     * @param size nombre d'opcions disponibles
     * @return índex nou
     */
    private static int moveRight(int selected, int size) {
        return Math.min(size - 1, selected + 1);
    }

    /**
     * Mou la selecció una fila cap amunt dins la graella.
     *
     * @param selected índex actual
     * @param columns columnes visibles
     * @return índex nou
     */
    private static int moveUp(int selected, int columns) {
        return Math.max(0, selected - columns);
    }

    /**
     * Mou la selecció una fila cap avall dins la graella.
     *
     * @param selected índex actual
     * @param columns columnes visibles
     * @param size nombre d'opcions disponibles
     * @return índex nou
     */
    private static int moveDown(int selected, int columns, int size) {
        return Math.min(size - 1, selected + columns);
    }

    /**
     * Accions que el lector de tecles pot lliurar al selector.
     */
    private enum Action {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        SELECT,
        CANCEL,
        IGNORE
    }
}
