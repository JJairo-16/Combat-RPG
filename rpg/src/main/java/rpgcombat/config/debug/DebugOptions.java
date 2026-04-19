package rpgcombat.config.debug;

/** Opcions de depuració. */
public record DebugOptions(
                boolean enabled,
                boolean forceInvulnerability,
                boolean preloadWithoutIntro) {

        public static DebugOptions getFalse() {
                return new DebugOptions(false, false, false);
        }
}
