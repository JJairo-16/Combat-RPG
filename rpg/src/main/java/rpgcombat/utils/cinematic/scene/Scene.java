package rpgcombat.utils.cinematic.scene;

import java.util.List;

/**
 * Representa una escena composta per diversos blocs de text.
 */
public class Scene {
    private final List<TextBlock> blocks;
    private final boolean clearBefore;
    private final boolean waitAtEnd;

    /**
     * Constructor intern a partir del builder.
     */
    Scene(SceneBuilder builder) {
        this.blocks = List.copyOf(builder.blocks);
        this.clearBefore = builder.clearBefore;
        this.waitAtEnd = builder.waitAtEnd;
    }

    /**
     * Retorna els blocs de text de l'escena.
     */
    public List<TextBlock> blocks() {
        return blocks;
    }

    /**
     * Indica si es neteja la pantalla abans de l'escena.
     */
    public boolean clearBefore() {
        return clearBefore;
    }

    /**
     * Indica si s'espera al final de l'escena.
     */
    public boolean waitAtEnd() {
        return waitAtEnd;
    }
}