package rpgcombat.weapons;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rpgcombat.weapons.config.WeaponConfig;
import rpgcombat.weapons.config.WeaponDefinition;
import rpgcombat.weapons.config.WeaponType;

/**
 * Lector de definicions d'armes des d'un JSON amb Gson.
 */
public final class WeaponLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WeaponLoader() {
    }

    public static List<WeaponDefinition> loadDefinitions(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WeaponConfig[] configs = GSON.fromJson(reader, WeaponConfig[].class);
            if (configs == null)
                return List.of();

            List<WeaponDefinition> list = new ArrayList<>(configs.length);
            for (WeaponConfig config : configs) {
                list.add(toDefinition(config));
            }
            return list;
        }
    }

    public static String toJson(List<WeaponConfig> configs) {
        return GSON.toJson(configs);
    }

    private static WeaponDefinition toDefinition(WeaponConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("WeaponConfig no pot ser null");
        }

        return new WeaponDefinition(
                cfg.id(),
                cfg.name(),
                cfg.description(),
                cfg.baseDamage(),
                cfg.criticalProb(),
                cfg.criticalDamage(),
                WeaponType.valueOf(cfg.type()),
                cfg.attackSkill(),
                cfg.manaPrice(),
                cfg.passives());
    }
}
