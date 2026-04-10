package rpgcombat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import menu.DynamicMenu;
import menu.action.MenuAction;
import menu.model.MenuResult;
import rpgcombat.combat.Action;
import rpgcombat.creator.CharacterCreator;
import rpgcombat.game.MenuBuilder;
import rpgcombat.game.modifier.MenuStatusModifier;
import rpgcombat.game.modifier.StatusMod;
import rpgcombat.game.modifier.config.StatusModConfig;
import rpgcombat.game.modifier.config.StatusModLoader;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

/**
 * Proves de càrrega i aplicació de modificadors d'estat des de JSON.
 */
class MenuStatusModifierJsonTest {

    private static final String RESOURCE_JSON = "/status-modifiers-test.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, MenuAction<Action, Character>> TEST_ACTIONS = Map.of(
            "ignite", c -> {
                // acció dummy per al test
                return MenuResult.repeatLoop();
            },
            "explode", c -> {
                // acció dummy per al test
                return MenuResult.repeatLoop();
            },
            "shatter", c -> {
                // acció dummy per al test
                return MenuResult.repeatLoop();
            });

    private Character player;
    private DynamicMenu<Action, Character> menu;

   /** Prepara el personatge i el menú base abans de cada prova. */
    @BeforeEach
    void setUp() {
        player = CharacterCreator.dummy();

        DynamicMenu<Action, Character> base = MenuBuilder.build(c -> {
        }, c -> {
        });

        menu = base.createChildMenu("menu-test", player);
        menu.saveCurrentAs("base");
    }

   /** Comprova que el JSON es llegeix i es transforma correctament. */
    @Test
    void shouldReadJsonAndBuildModifierMap() throws Exception {
        Path jsonPath = resourceToPath(RESOURCE_JSON);

        Map<String, List<StatusMod>> loaded = StatusModLoader.loadTest(jsonPath, TEST_ACTIONS);

        assertNotNull(loaded);
        assertFalse(loaded.isEmpty());
        assertTrue(loaded.containsKey("BURN"));
        assertTrue(loaded.containsKey("FREEZE"));
        assertEquals(2, loaded.get("BURN").size());
        assertEquals(1, loaded.get("FREEZE").size());
    }

   /** Comprova que el menú es modifica segons la configuració del JSON. */
    @Test
    void shouldModifyMenuAccordingToJsonConfig() throws Exception {
        Path jsonPath = resourceToPath(RESOURCE_JSON);

        Map<String, List<StatusMod>> loaded = StatusModLoader.loadTest(jsonPath, TEST_ACTIONS);
        Map<String, List<StatusModConfig>> rawConfigs = readRawJsonConfigs();

        int baseCount = menu.optionCount();

        // Efectes generats dinàmicament pensant que:
        // - A BURN hi entren els 2 mods del JSON
        // - A FREEZE hi entra el seu únic mod
        List<Effect> effects = List.of(
                new TestEffect("BURN", 3, 3, 2),
                new TestEffect("FREEZE", 2, 1, 0));

        for (Effect effect : effects) {
            player.addEffect(effect);
        }

        int expectedAdded = expectedMatchingMods(rawConfigs, effects);

        MenuStatusModifier modifier = new MenuStatusModifier(player, menu, loaded);
        modifier.mod("base");

        assertEquals(baseCount + expectedAdded, menu.optionCount());
    }

   /** Comprova que no es dupliquen opcions en reconstruir des d'un snapshot. */
    @Test
    void shouldNotDuplicateOptionsWhenRebuildingFromSnapshot() throws Exception {
        Path jsonPath = resourceToPath(RESOURCE_JSON);

        Map<String, List<StatusMod>> loaded = StatusModLoader.loadTest(jsonPath, TEST_ACTIONS);
        Map<String, List<StatusModConfig>> rawConfigs = readRawJsonConfigs();

        List<Effect> effects = List.of(
                new TestEffect("BURN", 3, 3, 2),
                new TestEffect("FREEZE", 2, 1, 0));

        for (Effect effect : effects) {
            player.addEffect(effect);
        }

        int baseCount = menu.optionCount();
        int expectedAdded = expectedMatchingMods(rawConfigs, effects);

        MenuStatusModifier modifier = new MenuStatusModifier(player, menu, loaded);

        modifier.mod("base");
        assertEquals(baseCount + expectedAdded, menu.optionCount());

        modifier.mod("base");
        assertEquals(baseCount + expectedAdded, menu.optionCount());
    }

   /** Comprova que s'ignoren els efectes expirats. */
    @Test
    void shouldIgnoreExpiredEffectsEvenIfPresentInJson() throws Exception {
        Path jsonPath = resourceToPath(RESOURCE_JSON);

        Map<String, List<StatusMod>> loaded = StatusModLoader.loadTest(jsonPath, TEST_ACTIONS);
        Map<String, List<StatusModConfig>> rawConfigs = readRawJsonConfigs();

        int baseCount = menu.optionCount();

        List<Effect> effects = List.of(
                new TestEffect("BURN", 0, 3, 2), // expirat
                new TestEffect("FREEZE", 0, 1, 0) // expirat
        );

        for (Effect effect : effects) {
            player.addEffect(effect);
        }

        int expectedAdded = expectedMatchingMods(rawConfigs, effects);

        MenuStatusModifier modifier = new MenuStatusModifier(player, menu, loaded);
        modifier.mod("base");

        assertEquals(0, expectedAdded);
        assertEquals(baseCount, menu.optionCount());
    }

   /** Comprova que només s'apliquen els mods que compleixen els rangs. */
    @Test
    void shouldApplyOnlyModsMatchingJsonRanges() throws Exception {
        Path jsonPath = resourceToPath(RESOURCE_JSON);

        Map<String, List<StatusMod>> loaded = StatusModLoader.loadTest(jsonPath, TEST_ACTIONS);
        Map<String, List<StatusModConfig>> rawConfigs = readRawJsonConfigs();

        int baseCount = menu.optionCount();

        // Per a BURN:
        // - Hi ha d'entrar "Avivar flames"
        // - No hi ha d'entrar "Explosió ígnia" perquè stacks=2 i demana minStacks=3
        List<Effect> effects = List.of(
                new TestEffect("BURN", 3, 2, 2));

        for (Effect effect : effects) {
            player.addEffect(effect);
        }

        int expectedAdded = expectedMatchingMods(rawConfigs, effects);

        MenuStatusModifier modifier = new MenuStatusModifier(player, menu, loaded);
        modifier.mod("base");

        assertEquals(1, expectedAdded);
        assertEquals(baseCount + expectedAdded, menu.optionCount());
    }

   /** Llegeix el JSON cru i en normalitza el contingut. */
    private Map<String, List<StatusModConfig>> readRawJsonConfigs() throws Exception {
        Type mapType = new TypeToken<Map<String, List<StatusModConfig>>>() {
        }.getType();

        try (Reader reader = new InputStreamReader(
                MenuStatusModifierJsonTest.class.getResourceAsStream(RESOURCE_JSON),
                StandardCharsets.UTF_8)) {

            Map<String, List<StatusModConfig>> raw = GSON.fromJson(reader, mapType);

            if (raw == null) {
                return Map.of();
            }

            Map<String, List<StatusModConfig>> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, List<StatusModConfig>> entry : raw.entrySet()) {
                normalized.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
            }
            return Map.copyOf(normalized);
        }
    }

   /** Calcula quants mods compleixen les condicions dels efectes. */
    private int expectedMatchingMods(Map<String, List<StatusModConfig>> rawConfigs, List<Effect> effects) {
        List<StatusModConfig> pending = new ArrayList<>();

        for (Effect effect : effects) {
            if (effect == null || effect.isExpired()) {
                continue;
            }

            List<StatusModConfig> cfgs = rawConfigs.get(effect.key());
            if (cfgs == null || cfgs.isEmpty()) {
                continue;
            }

            for (StatusModConfig cfg : cfgs) {
                if (matches(effect.state(), cfg)) {
                    pending.add(cfg);
                }
            }
        }

        pending.sort(Comparator.comparingInt(StatusModConfig::priority).reversed());
        return pending.size();
    }

   /** Indica si l'estat compleix la configuració del mod. */
    private boolean matches(EffectState state, StatusModConfig mod) {
        return inRange(state.charges(), mod.minCharges(), mod.maxCharges())
                && inRange(state.stacks(), mod.minStacks(), mod.maxStacks())
                && inRange(state.remainingTurns(), mod.minRemainingTurns(), mod.maxRemainingTurns());
    }

   /** Comprova si un valor cau dins d'un rang. */
    private boolean inRange(int value, Integer min, Integer max) {
        if (min != null && value < min) {
            return false;
        }

        if (max != null && max == -1) {
            return true;
        }

        return max == null || value <= max;
    }

   /** Converteix un recurs del classpath a Path. */
    private Path resourceToPath(String resourcePath) throws Exception {
        URI uri = MenuStatusModifierJsonTest.class.getResource(resourcePath).toURI();
        return Path.of(uri);
    }

   /** Efecte de prova amb clau i estat definits pel test. */
    private static final class TestEffect implements Effect {
        private final String key;
        private final EffectState state;

       /** Crea un efecte de prova. */
        TestEffect(String key, int remainingTurns, int stacks, int charges) {
            this.key = key;
            this.state = new EffectState(charges, stacks, remainingTurns, 0);
        }

       /** Retorna la clau de l'efecte. */
        @Override
        public String key() {
            return key;
        }

       /** Retorna l'estat de l'efecte. */
        @Override
        public EffectState state() {
            return state;
        }

       /** Indica si l'efecte ha expirat. */
        @Override
        public boolean isExpired() {
            return state.remainingTurns() <= 0;
        }
    }
}