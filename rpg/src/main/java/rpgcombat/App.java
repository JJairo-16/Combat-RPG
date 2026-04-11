package rpgcombat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.GameLoop;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.input.WeaponMenu;
import rpgcombat.utils.rng.D20Terminal;
import rpgcombat.utils.ui.LoadingIntro;
import rpgcombat.utils.ui.Prettier;
import rpgcombat.weapons.Arsenal;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private static final String WEAPONS_CONFIG_PATH = "rpg/data/weapons.json";
    private static final String STATUS_MENU_MODIFIER_PATH = "rpg/data/menuModifiers.json";

    private static final boolean DEBUG_MODE = true;

    private Map<String, List<StatusMod>> modifiers;

    private void preload() {
        try {
            Arsenal.preload(Path.of(WEAPONS_CONFIG_PATH));
            WeaponMenu.preloadCards();

            modifiers = StatusModLoader.load(Path.of(STATUS_MENU_MODIFIER_PATH));
        } catch (IOException e) {
            Prettier.error("Ha hagut un error durant la precarrega.");
        }

        D20Terminal.preloadFrames();
    }

    public void run() {
        GameLoop game = DEBUG_MODE ? newDebugGame() : newGame();
        game.init();
    }

    private GameLoop newDebugGame() {
        preload();
        ensureModifiersLoaded();

        Character p1 = CharacterCreator.createDebugCharacter();
        Character p2 = CharacterCreator.createDebugCharacter();
        return new GameLoop(p1, p2, modifiers);
    }

    private GameLoop newGame() {
        LoadingIntro intro = new LoadingIntro("Jairo Linares");
        intro.start(this::preload);
        ensureModifiersLoaded();

        Character p1 = CharacterCreator.createNewCharacter();
        Character p2 = CharacterCreator.createNewCharacter();
        return new GameLoop(p1, p2, modifiers);
    }

    private void ensureModifiersLoaded() {
        Objects.requireNonNull(modifiers, "No s'han carregat els modificadors correctament.");
    }
}