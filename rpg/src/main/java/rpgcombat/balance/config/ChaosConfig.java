package rpgcombat.balance.config;

import java.util.Map;

/**
 * Configura el trigger permanent de Caos.
 */
public record ChaosConfig(
        boolean enabled,
        SeedConfig seed,
        AntiFrustrationConfig antiFrustration,
        DamageConfig damage,
        CritConfig crit,
        ManaConfig mana,
        StatusConfig status,
        Map<String, Integer> outcomes) {

    /**
     * Configura les llavors de les tirades de Caos.
     */
    public record SeedConfig(
            long combatSalt,
            long ownerSalt,
            long turnSalt) {
    }

    /**
     * Defineix límits per evitar resultats massa frustrants.
     */
    public record AntiFrustrationConfig(
            boolean noRepeatOutcome,
            boolean noDoubleSevere,
            int maxRerolls) {
    }

    /**
     * Configura els modificadors de dany de Caos.
     */
    public record DamageConfig(
            double upMultiplier,
            double downMultiplier,
            double overloadMultiplier,
            double overloadIncomingMultiplier,
            double selfHitMultiplier,
            boolean selfHitCanKill) {
    }

    /**
     * Configura els efectes de Caos sobre els crítics.
     */
    public record CritConfig(
            double flipForceChance) {
    }

    /**
     * Configura la recuperació de manà de Caos.
     */
    public record ManaConfig(
            double spikeRestoreMaxManaRatio) {
    }

    /**
     * Configura les durades i probabilitats dels estats.
     */
    public record StatusConfig(
            int blindTurns,
            double blindMissChance,
            int bleedTurns,
            int staggerTurns,
            int vulnerableTurns,
            int fatigueTurns) {
    }
}