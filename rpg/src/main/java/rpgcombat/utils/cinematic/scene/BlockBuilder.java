package rpgcombat.utils.cinematic.scene;

import rpgcombat.utils.cinematic.style.CinematicColor;
import rpgcombat.utils.cinematic.typing.TypingMood;

/**
 * Constructor de blocs de text.
 */
public class BlockBuilder {
    String text = "";
    CinematicColor color = CinematicColor.WHITE;
    TypingMood mood = TypingMood.NORMAL;
    boolean smartTyping = true;
    boolean newLineAfter = true;

    long baseDelayMillis = 24;
    long randomVariationMillis = 40;
    long shortPauseMillis = 110;
    long mediumPauseMillis = 150;
    long longPauseMillis = 260;
    long ellipsisFlowPauseMillis = 25;
    long ellipsisEndPauseMillis = 280;

    private BlockBuilder(String text) {
        this.text = text == null ? "" : text;
    }

    /**
     * Crea un bloc amb el text indicat.
     */
    public static BlockBuilder text(String text) {
        return new BlockBuilder(text);
    }

    /**
     * Defineix el color inicial.
     */
    public BlockBuilder color(CinematicColor color) {
        this.color = color;
        return this;
    }

    /**
     * Defineix el to inicial.
     */
    public BlockBuilder mood(TypingMood mood) {
        this.mood = mood;
        return this;
    }

    /**
     * Activa o desactiva l'anàlisi intel·ligent.
     */
    public BlockBuilder smartTyping(boolean value) {
        this.smartTyping = value;
        return this;
    }

    /**
     * Decideix si fa salt de línia després.
     */
    public BlockBuilder newLineAfter(boolean value) {
        this.newLineAfter = value;
        return this;
    }

    /**
     * Defineix la velocitat base.
     */
    public BlockBuilder speed(long baseDelayMillis) {
        this.baseDelayMillis = baseDelayMillis;
        return this;
    }

    /**
     * Defineix la variació humana.
     */
    public BlockBuilder humanVariation(long millis) {
        this.randomVariationMillis = millis;
        return this;
    }

    /**
     * Defineix les pauses de puntuació.
     */
    public BlockBuilder punctuationPauses(long shortPauseMillis, long mediumPauseMillis, long longPauseMillis) {
        this.shortPauseMillis = shortPauseMillis;
        this.mediumPauseMillis = mediumPauseMillis;
        this.longPauseMillis = longPauseMillis;
        return this;
    }

    /**
     * Defineix el ritme dels punts suspensius.
     */
    public BlockBuilder ellipsisPauses(long flowMillis, long endMillis) {
        this.ellipsisFlowPauseMillis = flowMillis;
        this.ellipsisEndPauseMillis = endMillis;
        return this;
    }

    /**
     * Construeix el bloc.
     */
    public TextBlock build() {
        return new TextBlock(this);
    }
}
