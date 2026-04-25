package rpgcombat.utils.cinematic.typing;

import org.jline.terminal.Terminal;

import rpgcombat.utils.cinematic.terminal.CinematicInput;
import rpgcombat.utils.cinematic.terminal.TerminalController;

import java.io.IOException;
import java.util.List;

/**
 * Executa les accions d'escriptura a la terminal.
 */
public class TypingEngine {
    private static final long MIN_PLAYBACK_BEFORE_INPUT_MILLIS = 20;
    private static final long RELEASE_QUIET_MILLIS = 20;

    private final Terminal terminal;
    private final CinematicInput input;

    /**
     * Crea el motor d'escriptura.
     */
    public TypingEngine(Terminal terminal) {
        this.terminal = terminal;
        this.input = new CinematicInput(terminal);
    }

    /**
     * Escriu les accions i retorna si s'ha saltat la cinemàtica.
     */
    public boolean play(List<TypingAction> actions) throws IOException, InterruptedException {
        input.resetInputGate(MIN_PLAYBACK_BEFORE_INPUT_MILLIS, RELEASE_QUIET_MILLIS);

        for (int i = 0; i < actions.size(); i++) {
            CinematicInput.Action inputAction = input.readFreshActionIfAvailable();

            if (inputAction == CinematicInput.Action.ENTER) {
                TerminalController.resetColor(terminal);
                return true;
            }

            if (inputAction == CinematicInput.Action.SPACE) {
                printRemaining(actions, i);
                return false;
            }

            execute(actions.get(i), true);
        }

        return false;
    }

    /**
     * Imprimeix les accions restants sense retard.
     */
    private void printRemaining(List<TypingAction> actions, int startIndex) throws InterruptedException {
        for (int i = startIndex; i < actions.size(); i++) {
            TypingAction action = actions.get(i);

            if (action.type() == ActionType.PAUSE) {
                continue;
            }

            execute(action, false);
        }
    }

    /**
     * Executa una acció individual.
     */
    private void execute(TypingAction action, boolean applyDelay) throws InterruptedException {
        switch (action.type()) {
            case PRINT -> {
                terminal.writer().print(action.character());
                terminal.writer().flush();

                if (applyDelay) {
                    Thread.sleep(action.delayMillis());
                }
            }
            case PAUSE -> {
                if (applyDelay) {
                    Thread.sleep(action.delayMillis());
                }
            }
            case NEW_LINE -> {
                terminal.writer().println();
                terminal.writer().flush();
            }
            case COLOR -> TerminalController.color(terminal, action.color());
        }
    }
}
