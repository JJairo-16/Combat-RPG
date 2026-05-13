package rpgcombat.weapons.config;

import java.util.List;

/** DTO directe del JSON per a les regles de desbloqueig d'una arma. */
public record UnlockConfig(
        String mode,
        List<UnlockRequirementConfig> requirements) {
}
