package rpgcombat.gamemode.ui;

import java.util.List;

import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.utils.interactive.SimpleMenu;

/** Menú bàsic per escollir mode de joc abans de crear la partida. */
public final class GameModeSelectionMenu {
    private GameModeSelectionMenu() {
    }

    public static GameModeDefinition show(List<GameModeDefinition> modes, String defaultModeId) {
        List<GameModeDefinition> available = modes == null || modes.isEmpty()
                ? List.of(GameModeRegistry.getOrDefault(defaultModeId))
                : List.copyOf(modes);

        if (available.size() == 1) {
            return available.get(0);
        }

        List<String> labels = available.stream()
                .map(GameModeSelectionMenu::label)
                .toList();

        int option = new SimpleMenu(1).getOption(labels, "Mode de joc");
        int index = Math.clamp(option - 1, 0, available.size() - 1);
        return available.get(index);
    }

    private static String label(GameModeDefinition mode) {
        if (mode == null) {
            return "Mode desconegut";
        }
        String description = mode.description();
        if (description == null || description.isBlank()) {
            return mode.name();
        }
        return mode.name() + " - " + description;
    }
}
