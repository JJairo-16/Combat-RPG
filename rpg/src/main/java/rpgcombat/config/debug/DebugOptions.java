package rpgcombat.config.debug;

/**
 * Opcions de depuració.
 */
public record DebugOptions(
        boolean enabled,
        boolean forceInvulnerability,
        boolean preloadWithoutIntro) {
}
