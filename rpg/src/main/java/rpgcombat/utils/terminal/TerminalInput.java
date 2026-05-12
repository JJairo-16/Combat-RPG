package rpgcombat.utils.terminal;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.MouseSupport;

/** Utilitats d'entrada compartides per pantalles interactives. */
public final class TerminalInput {
    private TerminalInput() {
    }

    /** Vincula les seqüències de ratolí conegudes a una acció ignorada. */
    public static <T> void bindMouseIgnore(KeyMap<T> map, Terminal terminal, T ignoreAction) {
        for (String key : MouseSupport.keys(terminal)) {
            if (key != null && !key.isEmpty()) {
                map.bind(ignoreAction, key);
            }
        }
    }

    /** Llegeix una acció consumint completament els events de ratolí. */
    public static <T> T readBindingIgnoringMouse(
            BindingReader reader,
            KeyMap<T> map,
            Terminal terminal,
            T ignoreAction) {

        T action = reader.readBinding(map);
        String binding = reader.getLastBinding();
        if (action == ignoreAction && isMousePrefix(terminal, binding)) {
            consumeMouseEvent(terminal, binding);
        }
        return action;
    }

    /** Consumeix la resta d'un event de ratolí si el prefix ja s'ha llegit. */
    public static boolean consumeMouseEvent(Terminal terminal, String prefix) {
        if (!isMousePrefix(terminal, prefix)) {
            return false;
        }
        try {
            terminal.readMouseEvent(prefix);
        } catch (RuntimeException ignored) {
            // Si arriba una seqüència incompleta, igualment no s'ha de propagar al menú.
        }
        return true;
    }

    /** Reconeix els prefixos de ratolí després d'haver llegit ESC i dos caràcters. */
    public static String mousePrefixAfterEscape(int first, int second) {
        if (first == '[' && second == '<') {
            return "\033[<";
        }
        if (first == '[' && second == 'M') {
            return "\033[M";
        }
        return null;
    }

    /** Indica si la seqüència llegida és un prefix de ratolí. */
    public static boolean isMousePrefix(Terminal terminal, String binding) {
        if (binding == null) {
            return false;
        }
        for (String key : MouseSupport.keys(terminal)) {
            if (binding.equals(key)) {
                return true;
            }
        }
        return false;
    }
}
