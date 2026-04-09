package rpgcombat.weapons.attack;

import java.util.Map;

import rpgcombat.weapons.Skills;

/**
 * Registre que associa un identificador textual amb la seva implementació d'atac.
 */
public final class AttackRegistry {
    private static final Map<String, Attack> ATTACKS = Map.of(
            "nothing", Skills::nothing,
            "explosiveShot", Skills::explosiveShot,
            "arcaneDisruption", Skills::arcaneDisruption,
            "luckyBallista", Skills::luckyBallista,
            "grimoriCipher", Skills::grimoriCipher,
            "perforatingThrow", Skills::perforatingThrow,
            "chronoWeave", Skills::chronoWeave,
            "crossCut", Skills::crossCut);

    private AttackRegistry() {
    }

    /**
     * Resol una clau d'atac al seu comportament associat.
     *
     * @param key Identificador textual de l'atac
     * @return Implementació de l'atac
     * @throws IllegalArgumentException si la clau no existeix
     */
    public static Attack resolve(String key) {
        Attack attack = ATTACKS.get(key);
        if (attack == null) {
            throw new IllegalArgumentException("Atac desconegut: " + key);
        }
        return attack;
    }
}