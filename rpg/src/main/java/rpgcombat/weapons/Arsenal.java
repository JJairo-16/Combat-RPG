package rpgcombat.weapons;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rpgcombat.weapons.config.WeaponDefinition;

/**
 * Catàleg dinàmic d'armes precarregat des de JSON.
 *
 * <p>Substitueix l'antic enum Arsenal mantenint una idea semblant:</n>
 * <ul>
 *   <li>precàrrega una sola vegada</li>
 *   <li>guarda plantilles/definicions en memòria</li>
 *   <li>cada crida a create(...) genera una Weapon nova</li>
 * </ul>
 */
public final class Arsenal {
    private static final Map<String, WeaponDefinition> BY_ID = new LinkedHashMap<>();
    private static final List<WeaponDefinition> SORTED = new ArrayList<>();
    private static List<String> namesList = List.of();
    private static boolean loaded = false;

    private Arsenal() {
    }

    /**
     * Precàrrega el catàleg d'armes des d'un JSON.
     * No rellegeix el fitxer en cada accés.
     */
    public static synchronized void preload(Path jsonPath) throws IOException {
        List<WeaponDefinition> definitions = WeaponLoader.loadDefinitions(jsonPath);
        preload(definitions);
    }

   /** Precàrrega el catàleg a partir d'una col·lecció ja resolta. */
    public static synchronized void preload(Collection<WeaponDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("No s'han proporcionat definicions de armes");
        }

        Map<String, WeaponDefinition> map = new LinkedHashMap<>();
        for (WeaponDefinition definition : definitions) {
            WeaponDefinition previous = map.put(definition.getId(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("ID d'arma repetida: " + definition.getId());
            }
        }

        BY_ID.clear();
        BY_ID.putAll(map);

        SORTED.clear();
        SORTED.addAll(BY_ID.values().stream()
                .sorted(Comparator
                        .comparing((WeaponDefinition w) -> w.getType().getName())
                        .thenComparing(WeaponDefinition::getName))
                .toList());

        namesList = SORTED.stream()
                .map(Arsenal::formatForMenu)
                .toList();

        loaded = true;
    }

    /** Permet recarregar explícitament el catàleg. */
    public static synchronized void reload(Path jsonPath) throws IOException {
        loaded = false;
        preload(jsonPath);
    }

    /** Indica si el catàleg ja està precarregat. */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Equivalent conceptual a Arsenal.values() de l'antic enum.
     * Retorna les plantilles, no instàncies de Weapon.
     */
    public static List<WeaponDefinition> values() {
        ensureLoaded();
        return List.copyOf(SORTED);
    }

    /** Retorna una definició pel seu id string. */
    public static WeaponDefinition getDefinition(String id) {
        ensureLoaded();
        WeaponDefinition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("No existeix l'arma amb l'id: " + id);
        }
        return definition;
    }

    /**
     * Crea una arma nova a partir del seu id.
     * Cada crida retorna una instància independent.
     */
    public static Weapon create(String id) {
        return getDefinition(id).create();
    }

   /** Compatibilitat amb l'antic patró de seleccionar una arma per índex de menú. */
    public static Weapon getWeaponByIdx(int idx) {
        ensureLoaded();
        if (idx < 0 || idx >= SORTED.size()) {
            throw new IllegalArgumentException("L'index està fora del rang.");
        }
        return SORTED.get(idx).create();
    }

    /** Llista ja preparada per al menú. */
    public static List<String> getNamesList() {
        ensureLoaded();
        return namesList;
    }

    /** Àlies més explícit per al menú. */
    public static List<String> getMenuEntries() {
        return getNamesList();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException(
                    "El catàleg d'armes no està carregat. Crida Arsenal.preload(...) abans d'accedir-hi.");
        }
    }

    private static String formatForMenu(WeaponDefinition w) {
        String stats = String.format(
                "Tipus: %s | Dany: %d | Crit: %.0f%% | Mult: x%.2f%s",
                w.getType().getName(),
                w.getBaseDamage(),
                w.getCriticalProb() * 100.0,
                w.getCriticalDamage(),
                (w.getManaPrice() > 0) ? String.format(" | Mana: %.0f", w.getManaPrice()) : "");

        return w.getName() + " - " + w.getDescription() + " (" + stats + ")";
    }
}
