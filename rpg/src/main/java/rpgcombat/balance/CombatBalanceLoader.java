package rpgcombat.balance;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

import rpgcombat.balance.config.CombatBalanceConfig;

/**
 * Carrega la configuració d'equilibri de combat des de fitxer o classpath.
 */
public final class CombatBalanceLoader {
    private static final Gson GSON = new Gson();

    private CombatBalanceLoader() {
    }

    /**
     * Llegeix i valida la configuració des d'un fitxer.
     *
     * @param path ruta del fitxer
     * @return configuració carregada
     */
    public static CombatBalanceConfig load(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CombatBalanceConfig config = GSON.fromJson(reader, CombatBalanceConfig.class);
            CombatBalanceValidator.validate(config);
            return config;
        } catch (IOException e) {
            throw new CombatBalanceException("No s'ha pogut llegir el fitxer d'equilibri de combat: " + path, e);
        }
    }

    /**
     * Llegeix i valida la configuració des del classpath.
     *
     * @param resourcePath ruta del recurs
     * @return configuració carregada
     */
    public static CombatBalanceConfig loadFromClasspath(String resourcePath) {
        try (InputStream in = CombatBalanceLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new CombatBalanceException("Recurs d'equilibri de combat no trobat: " + resourcePath);
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                CombatBalanceConfig config = GSON.fromJson(reader, CombatBalanceConfig.class);
                CombatBalanceValidator.validate(config);
                return config;
            }
        } catch (IOException e) {
            throw new CombatBalanceException("No s'ha pogut llegir el recurs d'equilibri de combat: " + resourcePath, e);
        }
    }
}