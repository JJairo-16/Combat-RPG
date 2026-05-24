package rpgcombat.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import rpgcombat.terrain.model.TerrainSelectionMode;

/** Proves de persistència dels ajustos de l'usuari. */
class UserSettingsStoreTest {
    @Test
    void shouldSaveAndLoadMomentumMessagePreference() throws Exception {
        Path tempDir = Files.createTempDirectory("rpg-settings-test");
        Path defaults = tempDir.resolve("defaults.json");
        Files.writeString(defaults, "{\"showMomentumMessages\":true}");
        UserSettingsStore store = new UserSettingsStore(defaults, tempDir.resolve("settings.json"));

        store.save(new UserSettings(false, TerrainSelectionMode.MANUAL));

        assertFalse(store.load().showMomentumMessages());
        org.junit.jupiter.api.Assertions.assertEquals(TerrainSelectionMode.MANUAL,
                store.load().terrainSelectionMode());
        assertTrue(Files.exists(store.resolveAppDataPath()));

        String saved = Files.readString(store.resolveAppDataPath());
        assertFalse(saved.contains("version"));
        assertFalse(saved.contains("options"));
    }

    @Test
    void shouldUseDefaultsWhenFileDoesNotExist() throws Exception {
        Path tempDir = Files.createTempDirectory("rpg-settings-test");
        Path defaults = tempDir.resolve("defaults.json");
        Files.writeString(defaults, "{\"showMomentumMessages\":true}");
        UserSettingsStore store = new UserSettingsStore(defaults, tempDir.resolve("missing.json"));

        assertTrue(store.load().showMomentumMessages());
        org.junit.jupiter.api.Assertions.assertEquals(TerrainSelectionMode.NONE, store.load().terrainSelectionMode());
    }

    @Test
    void shouldFillMissingUserValuesWithDefaults() throws Exception {
        Path tempDir = Files.createTempDirectory("rpg-settings-test");
        Path defaults = tempDir.resolve("defaults.json");
        Path settings = tempDir.resolve("settings.json");
        Files.writeString(defaults, "{\"showMomentumMessages\":false}");
        Files.writeString(settings, "{}");

        UserSettingsStore store = new UserSettingsStore(defaults, settings);

        assertFalse(store.load().showMomentumMessages());
        org.junit.jupiter.api.Assertions.assertEquals(TerrainSelectionMode.NONE, store.load().terrainSelectionMode());
    }
}
