package rpgcombat.utils.cinematic.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constructor d'escenes.
 */
public class SceneBuilder {
    final List<TextBlock> blocks = new ArrayList<>();
    boolean clearBefore = true;
    boolean waitAtEnd = true;

    private SceneBuilder() {
    }

    /**
     * Crea un constructor d'escena.
     */
    public static SceneBuilder create() {
        return new SceneBuilder();
    }

    /**
     * Decideix si es neteja la pantalla abans.
     */
    public SceneBuilder clearBefore(boolean value) {
        this.clearBefore = value;
        return this;
    }

    /**
     * Decideix si espera al final.
     */
    public SceneBuilder waitAtEnd(boolean value) {
        this.waitAtEnd = value;
        return this;
    }

    /**
     * Afegeix un bloc.
     */
    public SceneBuilder block(TextBlock block) {
        blocks.add(block);
        return this;
    }

    /**
     * Afegeix diversos blocs.
     */
    public SceneBuilder blocks(TextBlock... blocks) {
        this.blocks.addAll(Arrays.asList(blocks));
        return this;
    }

    /**
     * Construeix l'escena.
     */
    public Scene build() {
        return new Scene(this);
    }
}
