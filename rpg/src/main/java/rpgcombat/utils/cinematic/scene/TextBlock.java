package rpgcombat.utils.cinematic.scene;

import rpgcombat.utils.cinematic.style.CinematicColor;
import rpgcombat.utils.cinematic.typing.TypingMood;

/**
 * Representa un bloc de text dins d'una escena.
 */
public class TextBlock {
    private final String text;
    private final CinematicColor color;
    private final TypingMood mood;
    private final boolean smartTyping;
    private final boolean newLineAfter;

    private final long baseDelayMillis;
    private final long randomVariationMillis;
    private final long shortPauseMillis;
    private final long mediumPauseMillis;
    private final long longPauseMillis;
    private final long ellipsisFlowPauseMillis;
    private final long ellipsisEndPauseMillis;

    /**
     * Constructor intern a partir del builder.
     */
    TextBlock(BlockBuilder builder) {
        this.text = normalize(builder.text);
        this.color = builder.color;
        this.mood = builder.mood;
        this.smartTyping = builder.smartTyping;
        this.newLineAfter = builder.newLineAfter;
        this.baseDelayMillis = builder.baseDelayMillis;
        this.randomVariationMillis = builder.randomVariationMillis;
        this.shortPauseMillis = builder.shortPauseMillis;
        this.mediumPauseMillis = builder.mediumPauseMillis;
        this.longPauseMillis = builder.longPauseMillis;
        this.ellipsisFlowPauseMillis = builder.ellipsisFlowPauseMillis;
        this.ellipsisEndPauseMillis = builder.ellipsisEndPauseMillis;
    }

    /**
     * Retorna el text del bloc.
     */
    public String text() { return text; }

    /**
     * Retorna el color inicial del bloc.
     */
    public CinematicColor color() { return color; }

    /**
     * Retorna el to d'escriptura del bloc.
     */
    public TypingMood mood() { return mood; }

    /**
     * Indica si s'aplica anàlisi intel·ligent.
     */
    public boolean smartTyping() { return smartTyping; }

    /**
     * Indica si s'afegeix salt de línia al final.
     */
    public boolean newLineAfter() { return newLineAfter; }

    /**
     * Retorna el retard base per caràcter.
     */
    public long baseDelayMillis() { return baseDelayMillis; }

    /**
     * Retorna la variació aleatòria del retard.
     */
    public long randomVariationMillis() { return randomVariationMillis; }

    /**
     * Retorna la pausa curta (comes, etc.).
     */
    public long shortPauseMillis() { return shortPauseMillis; }

    /**
     * Retorna la pausa mitjana (punts).
     */
    public long mediumPauseMillis() { return mediumPauseMillis; }

    /**
     * Retorna la pausa llarga (finals forts).
     */
    public long longPauseMillis() { return longPauseMillis; }

    /**
     * Retorna el retard entre punts suspensius.
     */
    public long ellipsisFlowPauseMillis() { return ellipsisFlowPauseMillis; }

    /**
     * Retorna la pausa final dels punts suspensius.
     */
    public long ellipsisEndPauseMillis() { return ellipsisEndPauseMillis; }

    /**
     * Normalitza el text eliminant indentació i espais finals.
     */
    private static String normalize(String text) {
        return text.stripIndent().stripTrailing();
    }
}