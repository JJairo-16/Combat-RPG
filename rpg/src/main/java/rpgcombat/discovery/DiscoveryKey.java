package rpgcombat.discovery;

import java.util.Objects;

/** Clau estable d'un descobriment dins una categoria. */
public record DiscoveryKey(DiscoveryCategory category, String id) {
    public DiscoveryKey {
        Objects.requireNonNull(category, "La categoria del descobriment no pot ser nul·la.");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'id del descobriment no pot estar buit.");
        }
        id = id.trim();
    }

    /** Clau compacta útil per mapes i fitxers. */
    public String compact() {
        return category.name() + ":" + id;
    }
}
