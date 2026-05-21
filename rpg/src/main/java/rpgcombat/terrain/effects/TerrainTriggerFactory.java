package rpgcombat.terrain.effects;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.regex.Pattern;

import rpgcombat.models.effects.Effect;
import rpgcombat.terrain.model.TerrainTriggerDefinition;

/**
 * Crea triggers personalitzats des del paquet reservat als terrenys.
 */
final class TerrainTriggerFactory {
    private static final String TRIGGER_PACKAGE = "rpgcombat.terrain.effects.triggers";
    private static final Pattern FILENAME_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    private TerrainTriggerFactory() {
    }

    /**
     * Carrega la classe indicada per la definició i en construeix una instància.
     *
     * @param definition definició JSON del trigger personalitzat
     * @return efecte executable o {@code null} si no hi ha definició
     */
    static Effect create(TerrainTriggerDefinition definition) {
        if (definition == null) {
            return null;
        }

        String simpleName = normalizeFileName(definition.fileName());
        String className = TRIGGER_PACKAGE + "." + simpleName;
        try {
            Class<?> rawType = Class.forName(className);
            if (!Effect.class.isAssignableFrom(rawType)) {
                throw new IllegalArgumentException("El trigger de terreny " + simpleName + " no implementa Effect.");
            }
            @SuppressWarnings("unchecked")
            Class<? extends Effect> effectType = (Class<? extends Effect>) rawType;
            return instantiate(effectType, definition);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("No s'ha pogut crear el trigger de terreny: " + simpleName, ex);
        }
    }

    private static Effect instantiate(Class<? extends Effect> type, TerrainTriggerDefinition definition)
            throws ReflectiveOperationException {
        try {
            Constructor<? extends Effect> constructor = type.getDeclaredConstructor(Map.class);
            return constructor.newInstance(definition.parameters());
        } catch (NoSuchMethodException ignored) {
            // Prova la següent signatura suportada.
        }

        try {
            Constructor<? extends Effect> constructor = type.getDeclaredConstructor(TerrainTriggerDefinition.class);
            return constructor.newInstance(definition);
        } catch (NoSuchMethodException ignored) {
            // Prova la següent signatura suportada.
        }

        try {
            Constructor<? extends Effect> constructor = type.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "El trigger de terreny " + type.getSimpleName()
                            + " necessita un constructor Map<String, Double>, TerrainTriggerDefinition o buit.",
                    ex);
        }
    }

    private static String normalizeFileName(String value) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("El nom de fitxer del trigger de terreny no pot ser buit.");

        String name = value.trim();
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - ".java".length());
        } else if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - ".class".length());
        }

        if (!FILENAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Nom de fitxer de trigger de terreny no vàlid: " + value);
        }

        return name;
    }
}
