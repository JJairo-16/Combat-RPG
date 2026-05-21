package rpgcombat.terrain.ui;

import java.util.List;
import java.util.Objects;

import rpgcombat.terrain.model.TerrainDefinition;
import rpgcombat.utils.input.Menu;

/**
 * Selector de terreny de combat basat en el menú de consola compartit.
 */
public final class TerrainSelectionScreen {
    private TerrainSelectionScreen() {
    }

    /**
     * Mostra els terrenys disponibles i retorna l'opció escollida.
     *
     * @param terrains terrenys disponibles
     * @return terreny seleccionat o l'opció neutra quan es cancel·la
     */
    public static TerrainDefinition choose(List<TerrainDefinition> terrains) {
        List<TerrainDefinition> options = terrains == null ? List.of()
                : terrains.stream().filter(Objects::nonNull).toList();
        if (options.isEmpty()) {
            return TerrainDefinition.none();
        }

        List<String> labels = options.stream()
                .map(TerrainSelectionScreen::labelFor)
                .toList();
        int selected = Menu.getOption(labels, "Tria el terreny");
        if (selected < 1 || selected > options.size()) {
            return TerrainDefinition.none();
        }
        return options.get(selected - 1);
    }

    private static String labelFor(TerrainDefinition terrain) {
        if (terrain.shortDescription().isBlank()) {
            return terrain.name();
        }
        return terrain.name() + " - " + terrain.shortDescription();
    }
}
