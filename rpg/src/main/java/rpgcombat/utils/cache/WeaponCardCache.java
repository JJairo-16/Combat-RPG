package rpgcombat.utils.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.config.WeaponDefinition;

/**
 * Memòria cau de "cards" d'armes, indexada per una clau compacta.
 *
 * Ara funciona amb Arsenal dinàmic (JSON) en comptes d'enum.
 */
public final class WeaponCardCache {

    private final String[] cache;

    /** Map per convertir ID → índex (equivalent a ordinal). */
    private final Map<String, Integer> indexById;

    public WeaponCardCache() {
        List<WeaponDefinition> defs = Arsenal.values();

        this.indexById = HashMap.newHashMap(defs.size());

        for (int i = 0; i < defs.size(); i++) {
            indexById.put(defs.get(i).getId(), i);
        }

        // 2 booleans → 4 combinacions
        this.cache = new String[defs.size() * 4];
    }

   /** Calcula la clau de memòria cau a partir de l'arma i banderes. */
    public int keyOf(WeaponDefinition def, boolean showEquipTag, boolean equippable) {
        if (def == null) {
            throw new IllegalArgumentException("weapon null");
        }

        Integer base = indexById.get(def.getId());
        if (base == null) {
            throw new IllegalArgumentException("weapon not registered: " + def.getId());
        }

        int key = base;
        key = (key << 1) | (showEquipTag ? 1 : 0);
        key = (key << 1) | (equippable ? 1 : 0);

        return key;
    }

   /** Overload útil si tens una Weapon en comptes de Definition */
    public int keyOf(String weaponId, boolean showEquipTag, boolean equippable) {
        Integer base = indexById.get(weaponId);
        if (base == null) {
            throw new IllegalArgumentException("weapon not registered: " + weaponId);
        }

        int key = base;
        key = (key << 1) | (showEquipTag ? 1 : 0);
        key = (key << 1) | (equippable ? 1 : 0);

        return key;
    }

    public String cardOf(int key) {
        return (key >= 0 && key < cache.length) ? cache[key] : null;
    }

    public void save(int key, String card) {
        if (key < 0 || key >= cache.length) {
            return;
        }
        cache[key] = card;
    }

    public void clear() {
        Arrays.fill(cache, null);
    }
}