package rpgcombat.utils.cinematic;

import java.io.IOException;

import rpgcombat.utils.cinematic.cinematic.TextCinematic;
import rpgcombat.utils.cinematic.scene.BlockBuilder;
import rpgcombat.utils.cinematic.scene.Scene;
import rpgcombat.utils.cinematic.scene.SceneBuilder;
import rpgcombat.utils.cinematic.scene.TextBlock;
import rpgcombat.utils.cinematic.typing.TypingMood;
import rpgcombat.utils.terminal.SharedTerminal;

/**
 * Exemple d'ús del sistema.
 */
public class Demo {
    public static void main(String[] args) throws IOException {
        SharedTerminal.preload();

        TextBlock ciutat = BlockBuilder
                .text("""
                        <gray>La nit queia sobre la ciutat...</gray>
                        <pause:300>
                        Els fanals parpellejaven com si alguna cosa respirés sota l'asfalt.
                        """)
                .mood(TypingMood.DRAMATIC)
                .build();

        TextBlock veu = BlockBuilder
                .text("""
                        <cyan>Llavors, una veu va xiuxiuejar:</cyan>
                        <br>
                        <bright_purple><slow>"Jugador... desperta."</slow></bright_purple>
                        """)
                .build();

        Scene escenaCiutat = SceneBuilder.create()
                .clearBefore(true)
                .blocks(ciutat)
                .build();

        Scene escenaVeu = SceneBuilder.create()
                .clearBefore(true)
                .blocks(veu)
                .build();

        TextCinematic intro = TextCinematic.builder()
                .clearScreenOnEnd(true)
                .arrowAnimationDelay(140)
                .scenes(escenaCiutat, escenaVeu)
                .build();

        intro.play();

        System.out.println("Comença el joc.");
    }
}
