package rpgcombat.utils.cinematic.cinematic;

import org.jline.terminal.Terminal;

import rpgcombat.utils.cinematic.scene.Scene;
import rpgcombat.utils.cinematic.scene.TextBlock;
import rpgcombat.utils.cinematic.terminal.CinematicInput;
import rpgcombat.utils.cinematic.terminal.TerminalController;
import rpgcombat.utils.cinematic.typing.TypingAnalyzer;
import rpgcombat.utils.cinematic.typing.TypingAction;
import rpgcombat.utils.cinematic.typing.TypingEngine;
import rpgcombat.utils.terminal.SharedTerminal;
import rpgcombat.utils.terminal.TerminalSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reprodueix una seqüència d'escenes de text amb animació.
 */
public class TextCinematic {
    private final List<PreparedScene> scenes;
    private final CinematicConfig config;

    /** Constructor intern. */
    private TextCinematic(Builder builder) {
        TypingAnalyzer analyzer = new TypingAnalyzer();
        this.scenes = builder.scenes.stream()
                .map(scene -> new PreparedScene(
                        scene,
                        scene.blocks().stream()
                                .map(block -> new PreparedBlock(block, analyzer.analyze(block)))
                                .toList()))
                .toList();
        this.config = builder.config;
    }

    /** Retorna un builder per crear la cinemàtica. */
    public static Builder builder() {
        return new Builder();
    }

    /** Inicia la reproducció de la cinemàtica. */
    public void play() {
        try (TerminalSession session = SharedTerminal.openSession()) {
            Terminal terminal = session.terminal();

            if (scenes.isEmpty() || !scenes.get(0).scene().clearBefore()) {
                TerminalController.clearScreen(terminal);
            }

            CinematicInput input = new CinematicInput(terminal);
            input.discardPendingInput();

            TypingEngine engine = new TypingEngine(terminal);

            for (PreparedScene scene : scenes) {
                if (scene.scene().clearBefore()) {
                    TerminalController.clearScreen(terminal);
                }

                boolean skipped = playScene(terminal, engine, scene);

                if (skipped) {
                    break;
                }
            }

            if (config.waitAtEnd) {
                waitForSpaceOrEscape(terminal);
            }

            if (config.clearScreenOnEnd) {
                TerminalController.clearScreen(terminal);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CinematicException("La cinemàtica s'ha interromput.", e);
        } catch (IOException e) {
            throw new CinematicException("No s'ha pogut reproduir la cinemàtica.", e);
        }
    }

    /** Reprodueix una escena. */
    private boolean playScene(Terminal terminal, TypingEngine engine, PreparedScene scene)
            throws IOException, InterruptedException {

        for (PreparedBlock block : scene.blocks()) {
            boolean skipped = engine.play(block.actions());

            if (skipped) {
                return true;
            }

            if (block.block().newLineAfter()) {
                terminal.writer().println();
                terminal.writer().flush();
            }
        }

        if (scene.scene().waitAtEnd()) {
            return waitForSpaceOrEscape(terminal);
        }

        return false;
    }

    private record PreparedScene(Scene scene, List<PreparedBlock> blocks) {
    }

    private record PreparedBlock(TextBlock block, List<TypingAction> actions) {
        private PreparedBlock {
            actions = List.copyOf(actions);
        }
    }

    /** Mostra animació d'espera fins que l'usuari interactua. */
    private boolean waitForSpaceOrEscape(Terminal terminal)
            throws IOException, InterruptedException {

        CinematicInput input = new CinematicInput(terminal);
        input.resetInputGate(20, 20);

        String[] frames = {
                "   ",
                "›  ",
                "›› ",
                "›››",
                " ››",
                "  ›"
        };

        int frame = 0;

        while (true) {
            terminal.writer().print("\r" + frames[frame]);
            terminal.writer().flush();

            frame = (frame + 1) % frames.length;

            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < config.arrowAnimationDelayMillis) {
                CinematicInput.Action action = input.readFreshActionIfAvailable();

                if (action == CinematicInput.Action.ENTER) {
                    terminal.writer().print("\r");
                    TerminalController.clearCurrentLine(terminal);
                    return true;
                }

                if (action == CinematicInput.Action.SPACE) {
                    terminal.writer().print("\r");
                    TerminalController.clearCurrentLine(terminal);
                    return false;
                }

                Thread.sleep(5);
            }
        }
    }

    /** Builder per construir la cinemàtica. */
    public static class Builder {
        private final List<Scene> scenes = new ArrayList<>();
        private final CinematicConfig config = new CinematicConfig();

        /** Configura la velocitat de l'animació d'espera. */
        public Builder arrowAnimationDelay(long millis) {
            config.arrowAnimationDelayMillis = millis;
            return this;
        }

        /** Indica si es neteja la pantalla al final. */
        public Builder clearScreenOnEnd(boolean value) {
            config.clearScreenOnEnd = value;
            return this;
        }

        public Builder waitAtEnd(boolean value) {
            config.waitAtEnd = value;
            return this;
        }

        /** Afegeix una escena. */
        public Builder scene(Scene scene) {
            scenes.add(scene);
            return this;
        }

        /** Afegeix múltiples escenes. */
        public Builder scenes(Scene... scenes) {
            this.scenes.addAll(Arrays.asList(scenes));
            return this;
        }

        /** Construeix la cinemàtica. */
        public TextCinematic build() {
            return new TextCinematic(this);
        }
    }

    /** Excepció de la cinemàtica. */
    public static class CinematicException extends RuntimeException {
        public CinematicException(String errorMessage, Throwable cause) {
            super(errorMessage, cause);
        }
    }
}
