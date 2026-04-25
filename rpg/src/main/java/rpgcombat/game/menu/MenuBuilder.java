package rpgcombat.game.menu;

import java.util.Objects;
import java.util.function.Consumer;

import menu.DynamicMenu;
import menu.model.MenuResult;
import menu.selector.MenuSelector;
import rpgcombat.combat.models.Action;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.input.Menu;

/**
 * Builder encarregat de crear el menú dinàmic d'accions del joc.
 * 
 * Les opcions són fixes (igual que a GameLoop), però el comportament
 * es delega mitjançant handlers externs.
 */
public final class MenuBuilder {

        private static final String OPTION_CHANGE_WEAPON = "Canviar arma";
        private static final String OPTION_PLAYER_INFO = "Veure informació";

        private MenuBuilder() {
        }

        /**
         * Crea el menú utilitzant el selector per defecte.
         *
         * @param changeWeaponHandler   acció per canviar l'arma
         * @param showPlayerInfoHandler acció per mostrar informació del jugador
         * @return menú configurat
         */
        public static DynamicMenu<Action, Character> build(
                        Consumer<Character> changeWeaponHandler,
                        Consumer<Character> showPlayerInfoHandler) {

                return build(
                                Menu::getOption,
                                changeWeaponHandler,
                                showPlayerInfoHandler);
        }

        /**
         * Crea el menú amb un selector personalitzat.
         *
         * @param selector              selector d'opcions
         * @param changeWeaponHandler   acció per canviar l'arma
         * @param showPlayerInfoHandler acció per mostrar informació del jugador
         * @return menú configurat
         */
        public static DynamicMenu<Action, Character> build(
                        MenuSelector selector,
                        Consumer<Character> changeWeaponHandler,
                        Consumer<Character> showPlayerInfoHandler) {

                Objects.requireNonNull(selector, "selector no pot ser null");
                Objects.requireNonNull(changeWeaponHandler, "changeWeaponHandler no pot ser null");
                Objects.requireNonNull(showPlayerInfoHandler, "showPlayerInfoHandler no pot ser null");

                DynamicMenu<Action, Character> menu = new DynamicMenu<>(
                                "Base Menu Actions",
                                CharacterCreator.dummy(),
                                selector);

                // Opció: canviar arma
                menu.addOption(OPTION_CHANGE_WEAPON, currentPlayer -> {
                        changeWeaponHandler.accept(currentPlayer);
                        return MenuResult.repeatLoop();
                });

                // Opcions que retornen acció de combat
                buildActions(menu);

                // Opció: veure informació del jugador
                menu.addOption(OPTION_PLAYER_INFO, currentPlayer -> {
                        showPlayerInfoHandler.accept(currentPlayer);
                        return MenuResult.repeatLoop();
                });

                // Hooks visuals
                menu.beforeEachAction(state -> System.out.println());

                menu.afterEachAction(state -> {
                        if (state.selectedOptionNumber() == 1)
                                Menu.pause();
                });

                return menu;
        }

        private static void buildActions(DynamicMenu<Action, Character> menu) {
                Action[] actions = Action.values();
                for (Action action : actions) {
                        menu.addOption(action.label(), currentPlayer -> MenuResult.returnValue(action));
                }
        }
}