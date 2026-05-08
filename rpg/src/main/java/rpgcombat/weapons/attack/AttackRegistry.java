package rpgcombat.weapons.attack;

import java.util.Map;

import rpgcombat.weapons.Skills;

/**
 * Registre que associa un identificador textual amb la seva implementació
 * d'atac.
 */
public final class AttackRegistry {
    private static final Map<String, Attack> ATTACKS = Map.ofEntries(
            Map.entry("nothing", Skills::nothing),
            Map.entry("explosiveShot", Skills::explosiveShot),
            Map.entry("arcaneDisruption", Skills::arcaneDisruption),
            Map.entry("luckyBallista", Skills::luckyBallista),
            Map.entry("grimoriCipher", Skills::grimoriCipher),
            Map.entry("perforatingThrow", Skills::perforatingThrow),
            Map.entry("chronoWeave", Skills::chronoWeave),
            Map.entry("crossCut", Skills::crossCut),
            Map.entry("elementalDuality", Skills::elementalDuality),
            Map.entry("firstOathStrike", Skills::firstOathStrike),
            Map.entry("firstBloodKnife", Skills::firstBloodKnife),
            Map.entry("brokenShieldBash", Skills::brokenShieldBash),
            Map.entry("ancestralBellEcho", Skills::ancestralBellEcho),
            Map.entry("chaosFragment", Skills::chaosFragment),
            Map.entry("tacticalMirrorCast", Skills::tacticalMirrorCast),
            Map.entry("retaliationShot", Skills::retaliationShot),
            Map.entry("badOmenSling", Skills::badOmenSling),
            Map.entry("coldStringShot", Skills::coldStringShot));

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
