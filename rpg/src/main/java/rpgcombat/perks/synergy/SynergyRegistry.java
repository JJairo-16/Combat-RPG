package rpgcombat.perks.synergy;

import java.util.ArrayList;
import java.util.List;

/** Registre global de definicions de sinergia. */
public final class SynergyRegistry {
    private static List<SynergyDefinition> definitions = List.of();

    private SynergyRegistry() {}

    /** Inicialitza el registre amb les sinergies carregades. */
    public static void initialize(List<SynergyDefinition> loaded) {
        definitions = loaded == null ? List.of() : List.copyOf(loaded);
    }

    /** Retorna totes les sinergies registrades. */
    public static List<SynergyDefinition> all() {
        return definitions;
    }

    /** Buida el registre. */
    public static void clear() {
        definitions = new ArrayList<>();
    }
}