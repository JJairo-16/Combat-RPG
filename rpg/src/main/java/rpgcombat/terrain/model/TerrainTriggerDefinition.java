package rpgcombat.terrain.model;

import java.util.Map;

/**
 * Delegació d'un terreny cap a un trigger personalitzat ja implementat.
 *
 * @param file nom del fitxer o de la classe del trigger dins del paquet de triggers de terreny
 * @param parameters paràmetres numèrics que rep el constructor del trigger
 */
public record TerrainTriggerDefinition(
        String file,
        Map<String, Double> parameters) {

    /**
     * Valida el fitxer de trigger i protegeix els paràmetres configurats.
     */
    public TerrainTriggerDefinition {
        file = file == null ? "" : file.trim();
        if (file.isBlank()) {
            throw new IllegalArgumentException("La delegació de terreny necessita un fitxer de trigger.");
        }
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    /**
     * Retorna el nom configurat del fitxer que ha de resoldre el factory.
     *
     * @return nom del fitxer o de la classe del trigger
     */
    public String fileName() {
        return file;
    }
}
