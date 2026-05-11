package rpgcombat.discovery;

/** Categories disponibles dins el grimori de descobriments. */
public enum DiscoveryCategory {
    GAME_MODES("Modes de joc"),

    BREEDS("Races"),
    WEAPONS("Armes"),
    ACTIONS("Accions"),
    EFFECTS("Efectes"),

    MISSIONS("Missions"),
    PERKS("Perks"),
    DIVINE_PERKS("Perks divines"),
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
