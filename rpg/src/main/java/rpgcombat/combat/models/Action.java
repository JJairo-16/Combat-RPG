package rpgcombat.combat.models;

import java.util.Objects;

import rpgcombat.combat.CombatSystem;

/**
 * Representa les accions bàsiques que un personatge pot realitzar durant un
 * torn de combat.
 *
 * <p>
 * Cada valor indica el comportament general que s'aplicarà en la resolució
 * del torn dins del {@link CombatSystem}.
 * </p>
 */
public enum Action {
    ATTACK("Atacar"),
    DEFEND("Defensar-se"),
    DODGE("Esquivar"),
    CHARGE("Carregar atac");

    private final String label;

    private Action(String label) {
        Objects.requireNonNull(label, "El label de la acció no pot ser nul·la.");
        if (label.isBlank())
            throw new IllegalArgumentException("La label no pot estar buida.");

        this.label = label.trim();
    }

    public String label() {
        return label;
    }
}