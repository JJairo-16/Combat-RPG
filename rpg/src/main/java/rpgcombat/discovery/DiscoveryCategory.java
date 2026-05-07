package rpgcombat.discovery;

/** Categories disponibles dins el grimori de descobriments. */
public enum DiscoveryCategory {
    WEAPONS("Armes"),
    BREEDS("Races"),
    PERKS("Perks"),
    DIVINE_PERKS("Perks divines"),
    MISSIONS("Missions"),
    ACTIONS("Accions"),
    EFFECTS("Efectes"),
    SYNERGIES("Sinergies");

    private final String defaultTitle;

    DiscoveryCategory(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    /** Títol català per defecte de la categoria. */
    public String defaultTitle() {
        return defaultTitle;
    }
}
