package rpgcombat;

import java.io.IOException;
import java.nio.file.Path;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.GameLoop;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.input.WeaponMenu;
import rpgcombat.utils.ui.LoadingIntro;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.Arsenal;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private static final String WEAPONS_CONFIG_PATH = "rpg/data/weapons.json";
    private static final boolean DEBUG_MODE = false;

    private void preload() {
        try {
            Arsenal.preload(Path.of(WEAPONS_CONFIG_PATH));
            WeaponMenu.preloadCards();
        } catch (IOException e) {
            Prettier.error("");
        }
    }

    public void run() {
        GameLoop game = DEBUG_MODE ? newDebugGame() : newGame();
        game.init();
    }

    private GameLoop newDebugGame() {
        preload();
        Character p1 = CharacterCreator.createDebugCharacter();
        Character p2 = CharacterCreator.createDebugCharacter();
        return new GameLoop(p1, p2);
    }

    private GameLoop newGame() {
        LoadingIntro intro = new LoadingIntro("Jairo Linares");
        intro.start(this::preload);

        Character p1 = CharacterCreator.createNewCharacter();
        Character p2 = CharacterCreator.createNewCharacter();
        return new GameLoop(p1, p2);
    }
}