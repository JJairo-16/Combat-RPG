package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.discovery.config.DiscoveryCatalog;
import rpgcombat.weapons.Arsenal;

class DiscoverySystemTest {
    @BeforeAll
    static void init() throws Exception {
        Arsenal.preload(Path.of("data/weapons.json"));
    }

    @Test
    void savesAccumulatedDiscoveriesWhenRoundStarts() throws Exception {
        Path savePath = Files.createTempDirectory("discovery-system-save").resolve("discoveries.json");
        DiscoveryCatalog catalog = DiscoveryCatalog.build(null);
        DiscoverySystem system = DiscoverySystem.load(catalog, savePath.toString());

        system.discover(DiscoveryCategory.WEAPONS, "EXPLOSIVE_CROSSBOW");

        assertTrue(system.isDiscovered(DiscoveryCategory.WEAPONS, "EXPLOSIVE_CROSSBOW"));
        assertFalse(Files.exists(savePath));

        system.onRoundStart();

        assertTrue(Files.exists(savePath));
        assertTrue(DiscoverySystem.load(catalog, savePath.toString())
                .isDiscovered(DiscoveryCategory.WEAPONS, "EXPLOSIVE_CROSSBOW"));
    }
}
