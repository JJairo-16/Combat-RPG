package rpgcombat.terrain.effects;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.regex.Pattern;

import rpgcombat.models.effects.Effect;
import rpgcombat.terrain.model.TerrainTriggerDefinition;

/**
 * Resol triggers personalitzats de terreny a partir del fitxer declarat al JSON.
 */
final class TerrainTriggerFactory {
    private static final String TRIGGER_PACKAGE = "rpgcombat.terrain.effects.triggers";
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    private TerrainTriggerFactory() {
    }

    /**
     * Crea l'efecte personalitzat declarat per un terreny.
     *
     * @param definition definició de delegació del terreny
     * @return efecte construït o {@code null} quan no hi ha definició
     */
    static Effect create(TerrainTriggerDefinition definition) {
        if (definition == null) {
            return null;
        }

        String simpleName = normalizeClassName(definition.fileName());
        String className = TRIGGER_PACKAGE + "." + simpleName;

        try {
            Class<?> rawType = Class.forName(className);

            if (!Effect.class.isAssignableFrom(rawType)) {
                throw new IllegalArgumentException(
                        "El trigger de terreny " + simpleName + " no implementa Effect."
                );
            }

            Class<? extends Effect> effectType = rawType.asSubclass(Effect.class);
            return instantiate(effectType, definition);

        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "No s'ha pogut crear el trigger de terreny: " + simpleName,
                    ex
            );
        }
    }

    /**
     * Instancia un trigger amb la signatura suportada que hagi declarat.
     *
     * @param type classe d'efecte resolta
     * @param definition paràmetres configurats pel terreny
     * @return efecte instanciat
     * @throws ReflectiveOperationException quan el constructor no es pot executar
     */
    private static Effect instantiate(Class<? extends Effect> type, TerrainTriggerDefinition definition)
            throws ReflectiveOperationException {

        Constructor<? extends Effect> constructor = findConstructor(type);

        Class<?>[] parameterTypes = constructor.getParameterTypes();

        if (parameterTypes.length == 0) {
            return constructor.newInstance();
        }

        if (parameterTypes[0] == Map.class) {
            return constructor.newInstance(definition.parameters());
        }

        return constructor.newInstance(definition);
    }

    /**
     * Cerca el constructor compatible amb la interfície de triggers de terreny.
     *
     * @param type classe d'efecte resolta
     * @return constructor suportat
     */
    private static Constructor<? extends Effect> findConstructor(Class<? extends Effect> type) {
        for (Class<?>[] signature : supportedSignatures()) {
            try {
                return type.getDeclaredConstructor(signature);
            } catch (NoSuchMethodException ignored) {
                // Continua provant signatures suportades.
            }
        }

        throw new IllegalArgumentException(
                "El trigger de terreny " + type.getSimpleName()
                        + " necessita un constructor Map, TerrainTriggerDefinition o buit."
        );
    }

    /**
     * Enumera les signatures que un trigger personalitzat pot exposar.
     *
     * @return signatures admeses per ordre de preferència
     */
    private static Class<?>[][] supportedSignatures() {
        return new Class<?>[][] {
                { Map.class },
                { TerrainTriggerDefinition.class },
                {}
        };
    }

    /**
     * Normalitza el nom de fitxer configurat fins al nom simple de classe.
     *
     * @param value nom declarat al JSON
     * @return nom simple validat
     */
    private static String normalizeClassName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nom de fitxer del trigger de terreny no pot ser buit.");
        }

        String name = value.trim()
                .replaceFirst("\\.java$", "")
                .replaceFirst("\\.class$", "");

        if (!CLASS_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Nom de fitxer de trigger de terreny no vàlid: " + value);
        }

        return name;
    }
}
