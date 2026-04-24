package rpgcombat.utils.cinematic.markup;

/**
 * Representa un token del llenguatge de tags.
 */
public class MarkupToken {

    /**
     * Tipus de token.
     */
    public enum Kind {
        TEXT,
        OPEN,
        CLOSE,
        INSTANT
    }

    private final Kind kind;
    private final String text;
    private final String name;
    private final String value;

    /**
     * Constructor intern del token.
     */
    private MarkupToken(Kind kind, String text, String name, String value) {
        this.kind = kind;
        this.text = text;
        this.name = name;
        this.value = value;
    }

    /**
     * Crea un token de text pla.
     */
    static MarkupToken text(String text) {
        return new MarkupToken(Kind.TEXT, text, null, null);
    }

    /**
     * Crea un token d'obertura de tag.
     */
    static MarkupToken open(String name) {
        return new MarkupToken(Kind.OPEN, null, name, null);
    }

    /**
     * Crea un token de tancament de tag.
     */
    static MarkupToken close(String name) {
        return new MarkupToken(Kind.CLOSE, null, name, null);
    }

    /**
     * Crea un token d'acció instantània.
     */
    static MarkupToken instant(String name, String value) {
        return new MarkupToken(Kind.INSTANT, null, name, value);
    }

    /**
     * Retorna el tipus de token.
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Retorna el text (si és TEXT).
     */
    public String text() {
        return text;
    }

    /**
     * Retorna el nom del tag.
     */
    public String name() {
        return name;
    }

    /**
     * Retorna el valor del tag (si en té).
     */
    public String value() {
        return value;
    }
}