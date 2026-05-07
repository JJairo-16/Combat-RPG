package rpgcombat.discovery;

import java.time.Instant;

/** Progrés persistent d'una entrada descoberta. */
public final record DiscoveryProgress(DiscoveryKey key, Instant firstDiscoveredAt) {
    public DiscoveryProgress(DiscoveryKey key, Instant firstDiscoveredAt) {
        this.key = key;
        this.firstDiscoveredAt = firstDiscoveredAt == null ? Instant.now() : firstDiscoveredAt;
    }

    /** Crea un progrés nou per a una entrada acabada de descobrir. */
    public static DiscoveryProgress newlyDiscovered(DiscoveryKey key) {
        return new DiscoveryProgress(key, Instant.now());
    }
}
