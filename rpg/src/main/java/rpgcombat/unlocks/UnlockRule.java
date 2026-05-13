package rpgcombat.unlocks;

import java.util.List;

/** Regla de desbloqueig associada a contingut del joc. */
public record UnlockRule(
        UnlockMode mode,
        List<UnlockRequirement> requirements) {

    public UnlockRule {
        mode = mode == null ? UnlockMode.ALL : mode;
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    public boolean openByDefault() {
        return requirements.isEmpty();
    }
}
